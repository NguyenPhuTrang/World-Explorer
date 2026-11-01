//đăng ký và kết nối Controller, Service cùng các dependency mà Users module cần sử dụng.
import { Module } from '@nestjs/common';
import { UsersController } from './users.controller.js';
import { UsersService } from './users.service.js';
import { PrismaModule } from '../prisma/prisma.module.js';
import { AuthModule } from '../auth/auth.module.js';

@Module({
  controllers: [UsersController],
  providers: [UsersService],
  imports: [PrismaModule, AuthModule],
  exports: [UsersService]
})
export class UsersModule {}
