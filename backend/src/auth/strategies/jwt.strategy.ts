// xác thực Access Token (JWT) và xác định người dùng đang thực hiện request là ai.
import {
  Injectable,
  UnauthorizedException,
} from '@nestjs/common';

import { ConfigService } from '@nestjs/config';
import { PassportStrategy } from '@nestjs/passport';
import { ExtractJwt, Strategy } from 'passport-jwt';

import { PrismaService } from '../../prisma/prisma.service.js';

interface JwtPayload {
  sub: string;
  role: string;
}

@Injectable()
export class JwtStrategy extends PassportStrategy(Strategy) {
  constructor(
    configService: ConfigService,
    private readonly prisma: PrismaService,
  ) {
    super({
      jwtFromRequest: ExtractJwt.fromAuthHeaderAsBearerToken(),
      ignoreExpiration: false,
      secretOrKey: configService.getOrThrow<string>(
        'JWT_ACCESS_SECRET',
      ),
    });
  }

  async validate(payload: JwtPayload) {
    if (!payload.sub) {
      throw new UnauthorizedException(
        'Access Token không hợp lệ',
      );
    }

    const user = await this.prisma.user.findFirst({
      where: {
        id: payload.sub,
        isActive: true,
        deletedAt: null,
      },
      select: {
        id: true,
        role: {
          select: {
            name: true,
          },
        },
      },
    });

    if (!user) {
      throw new UnauthorizedException(
        'Tài khoản không tồn tại hoặc đã bị vô hiệu hóa',
      );
    }

    return {
      userId: user.id,
      role: user.role.name,
    };
  }
}