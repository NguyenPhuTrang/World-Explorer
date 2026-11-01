
import {
  Body,
  Controller,
  Post,
  Req,
  Res,
} from '@nestjs/common';

import type {
  CookieOptions,
  Request,
  Response,
} from 'express';

import { AuthService } from './auth.service.js';
import { GoogleLoginDto } from './dto/google-login.dto.js';

@Controller('auth')
export class AuthController {
  constructor(
    private readonly authService: AuthService,
  ) {}

  // Cấu hình cookie Refresh Token
  private getRefreshCookieOptions(): CookieOptions {
    const sameSite =
      process.env.COOKIE_SAME_SITE === 'none'
        ? 'none'
        : 'lax';

    const secure =
      process.env.NODE_ENV === 'production' ||
      sameSite === 'none';

    return {
      httpOnly: true,
      secure,
      sameSite,
      path: '/api/v1/auth',
      maxAge: 7 * 24 * 60 * 60 * 1000,
    };
  }

  // 1. GOOGLE LOGIN

  @Post('google')
  async googleLogin(
    @Body() dto: GoogleLoginDto,
    @Req() req: Request,
    @Res({ passthrough: true }) res: Response,
  ) {
    const result = await this.authService.googleLogin(
      dto.idToken,
      req.get('user-agent'),
      req.ip,
    );

    // Lưu Refresh Token vào HttpOnly Cookie
    res.cookie(
      'refresh_token',
      result.refreshToken,
      this.getRefreshCookieOptions(),
    );

    // Không trả Refresh Token trong JSON
    return {
      accessToken: result.accessToken,
      user: result.user,
    };
  }

  // 2. REFRESH ACCESS TOKEN
  @Post('refresh')
  async refresh(
    @Req() req: Request,
    @Res({ passthrough: true }) res: Response,
  ) {
    const refreshToken = req.cookies?.refresh_token;

    const result = await this.authService.refreshAccessToken(
      refreshToken,
      req.get('user-agent'),
      req.ip,
    );

    // Thay cookie cũ bằng Refresh Token mới
    res.cookie(
      'refresh_token',
      result.refreshToken,
      this.getRefreshCookieOptions(),
    );

    return {
      accessToken: result.accessToken,
    };
  }

  // 3. LOGOUT

    @Post('logout')
    async logout(
    @Req() req: Request,
    @Res({ passthrough: true }) res: Response,
    ) {
    const refreshToken = req.cookies?.refresh_token;

    // Thu hồi Refresh Token trong database
    await this.authService.logout(refreshToken);

    // Xóa Refresh Token Cookie khỏi trình duyệt
    const cookieOptions = this.getRefreshCookieOptions();

    res.clearCookie('refresh_token', {
        httpOnly: cookieOptions.httpOnly,
        secure: cookieOptions.secure,
        sameSite: cookieOptions.sameSite,
        path: cookieOptions.path,
    });

    return {
        message: 'Đã đăng xuất',
    };
    }

}
