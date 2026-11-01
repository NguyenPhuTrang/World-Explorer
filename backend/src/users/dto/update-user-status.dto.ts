//  DTO này dùng cho API khóa hoặc mở khóa tài khoản
import { IsBoolean } from 'class-validator';

export class UpdateUserStatusDto {
  @IsBoolean()
  isActive!: boolean;
}