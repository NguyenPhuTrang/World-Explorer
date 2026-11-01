
import {
  Injectable,
  UnauthorizedException,
  InternalServerErrorException,
} from '@nestjs/common';

import { ConfigService } from '@nestjs/config';
import { JwtService } from '@nestjs/jwt';
import { OAuth2Client } from 'google-auth-library';

import { randomBytes, createHash } from 'node:crypto';

import { PrismaService } from '../prisma/prisma.service.js';

@Injectable()
export class AuthService {
  private readonly googleClient: OAuth2Client;

  private readonly refreshTokenLifetime = 7 * 24 * 60 * 60 * 1000; // token có thời hạn trong 7 ngày

  constructor(
    private readonly configService: ConfigService,
    private readonly prisma: PrismaService,
    private readonly jwtService: JwtService,
  ) {
    this.googleClient = new OAuth2Client(
      this.configService.getOrThrow<string>('GOOGLE_CLIENT_ID'),
    );
  }

  // 1. XÁC MINH GOOGLE ID TOKEN
  async verifyGoogleToken(idToken: string) {
    let ticket;

    try {
      ticket = await this.googleClient.verifyIdToken({
        idToken,
        audience: this.configService.getOrThrow<string>(
          'GOOGLE_CLIENT_ID',
        ),
      });
    } catch {
      throw new UnauthorizedException(
        'Google ID token không hợp lệ hoặc đã hết hạn',
      );
    }

    const payload = ticket.getPayload();

    if (
      !payload ||
      !payload.sub ||
      !payload.email ||
      payload.email_verified !== true
    ) {
      throw new UnauthorizedException(
        'Google account không hợp lệ hoặc email chưa được xác minh',
      );
    }

    return {
      googleId: payload.sub,
      email: payload.email.trim().toLowerCase(),
      fullName:
        payload.name ?? payload.email.split('@')[0],
      avatarUrl: payload.picture ?? null,
    };
  }

  // 2. TÌM HOẶC TẠO USER TỪ GOOGLE ACCOUNT
  async findOrCreateGoogleUser(profile: {
    googleId: string;
    email: string;
    fullName: string;
    avatarUrl: string | null;
  }) {
    const email = profile.email.trim().toLowerCase();

    return this.prisma.$transaction(async (tx) => {
      // Kiểm tra Google Account đã được liên kết chưa
      const oauthAccount = await tx.oAuthAccount.findUnique({
        where: {
          provider_providerAccountId: {
            provider: 'GOOGLE',
            providerAccountId: profile.googleId,
          },
        },
        include: {
          user: {
            include: {
              role: true,
            },
          },
        },
      });

      // Nếu đã liên kết, lấy User tương ứng
      if (oauthAccount) {
        const user = oauthAccount.user;

        if (!user.isActive || user.deletedAt) {
          throw new UnauthorizedException(
            'Tài khoản đã bị vô hiệu hóa',
          );
        }

        return user;
      }

      // Tìm User theo email
      const existingUser = await tx.user.findUnique({
        where: { email },
        include: { role: true },
      });

      if (existingUser) {
        if (!existingUser.isActive || existingUser.deletedAt) {
          throw new UnauthorizedException(
            'Tài khoản đã bị vô hiệu hóa',
          );
        }

        // Liên kết Google với User hiện có
        await tx.oAuthAccount.create({
          data: {
            provider: 'GOOGLE',
            providerAccountId: profile.googleId,
            userId: existingUser.id,
          },
        });

        return existingUser;
      }

      // Tìm Role CUSTOMER cho User mới
      const customerRole = await tx.role.findUnique({
        where: { name: 'CUSTOMER' },
      });

      if (!customerRole) {
        throw new InternalServerErrorException(
          'Role CUSTOMER chưa được khởi tạo',
        );
      }

      // Tạo User mới
      const newUser = await tx.user.create({
        data: {
          email,
          fullName: profile.fullName,
          avatarUrl: profile.avatarUrl,
          roleId: customerRole.id,
        },
        include: { role: true },
      });

      // Liên kết Google Account với User mới
      await tx.oAuthAccount.create({
        data: {
          provider: 'GOOGLE',
          providerAccountId: profile.googleId,
          userId: newUser.id,
        },
      });

      return newUser;
    });
  }

  // 3. TẠO ACCESS TOKEN
  async generateAccessToken(user: {
    id: string;
    role: {
      name: string;
    };
  }) {
    return this.jwtService.signAsync({
      sub: user.id,
      role: user.role.name,
    });
  }

  // 4. HASH REFRESH TOKEN
  private hashRefreshToken(token: string) {
    return createHash('sha256')
      .update(token)
      .digest('hex');
  }

  // 5. TẠO VÀ LƯU REFRESH TOKEN
  async generateRefreshToken(
    userId: string,
    userAgent?: string,
    ipAddress?: string,
  ) {
    // Tạo chuỗi Refresh Token ngẫu nhiên
    const refreshToken = randomBytes(64).toString('base64url');

    // Chỉ lưu hash vào database
    const tokenHash = this.hashRefreshToken(refreshToken);

    const expiresAt = new Date(
      Date.now() + this.refreshTokenLifetime,
    );

    await this.prisma.refreshToken.create({
      data: {
        userId,
        tokenHash,
        expiresAt,
        userAgent,
        ipAddress,
      },
    });

    // Trả token gốc cho Controller
    return refreshToken;
  }

  // 6. GOOGLE LOGIN HOÀN CHỈNH
  async googleLogin(
    idToken: string,
    userAgent?: string,
    ipAddress?: string,
  ) {
    // Xác minh Google ID Token
    const profile = await this.verifyGoogleToken(idToken);

    // Tìm hoặc tạo User
    const user = await this.findOrCreateGoogleUser(profile);

    // Tạo Access Token
    const accessToken = await this.generateAccessToken(user);

    // Tạo Refresh Token
    const refreshToken = await this.generateRefreshToken(
      user.id,
      userAgent,
      ipAddress,
    );

    return {
      accessToken,
      refreshToken,
      user: {
        id: user.id,
        email: user.email,
        fullName: user.fullName,
        avatarUrl: user.avatarUrl,
        role: user.role.name,
      },
    };
  }

  // 7. REFRESH TOKEN ROTATION
  async refreshAccessToken(
    refreshToken: string,
    userAgent?: string,
    ipAddress?: string,
  ) {
    if (!refreshToken) {
      throw new UnauthorizedException(
        'Refresh Token không được cung cấp',
      );
    }

    const tokenHash = this.hashRefreshToken(refreshToken);
    const now = new Date();

    // Thực hiện thu hồi token cũ và tạo token mới
    // trong cùng một transaction
    const result = await this.prisma.$transaction(async (tx) => {
      const storedToken = await tx.refreshToken.findUnique({
        where: { tokenHash },
        include: {
          user: {
            include: {
              role: true,
            },
          },
        },
      });

      if (
        !storedToken ||
        storedToken.revokedAt ||
        storedToken.expiresAt <= now
      ) {
        throw new UnauthorizedException(
          'Refresh Token không hợp lệ hoặc đã hết hạn',
        );
      }

      const user = storedToken.user;

      if (!user.isActive || user.deletedAt) {
        throw new UnauthorizedException(
          'Tài khoản đã bị vô hiệu hóa',
        );
      }

      // Thu hồi token cũ.
      // updateMany với điều kiện revokedAt: null giúp
      // ngăn hai request cùng sử dụng một token thành công.
      const revoked = await tx.refreshToken.updateMany({
        where: {
          id: storedToken.id,
          revokedAt: null,
          expiresAt: { gt: now },
        },
        data: {
          revokedAt: now,
          lastUsedAt: now,
        },
      });

      if (revoked.count !== 1) {
        throw new UnauthorizedException(
          'Refresh Token đã được sử dụng hoặc bị thu hồi',
        );
      }

      // Sinh Refresh Token mới
      const newRefreshToken = randomBytes(64).toString(
        'base64url',
      );

      const newTokenHash = this.hashRefreshToken(
        newRefreshToken,
      );

      const expiresAt = new Date(
        Date.now() + this.refreshTokenLifetime,
      );

      await tx.refreshToken.create({
        data: {
          userId: user.id,
          tokenHash: newTokenHash,
          expiresAt,
          userAgent,
          ipAddress,
        },
      });

      return {
        user,
        refreshToken: newRefreshToken,
      };
    });

    // Tạo Access Token mới
    const accessToken = await this.generateAccessToken(
      result.user,
    );

    return {
      accessToken,
      refreshToken: result.refreshToken,
    };
  }

  // 8. LOGOUT - THU HỒI REFRESH TOKEN
  async logout(refreshToken: string) {
    if (!refreshToken) {
      return { message: 'Đã đăng xuất' };
    }

    const tokenHash = this.hashRefreshToken(refreshToken);

    await this.prisma.refreshToken.updateMany({
      where: {
        tokenHash,
        revokedAt: null,
      },
      data: {
        revokedAt: new Date(),
      },
    });

    return {
      message: 'Đã đăng xuất',
    };
  }
}
