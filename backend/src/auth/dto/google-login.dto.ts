
import { IsNotEmpty, IsString } from 'class-validator'; // Data Transfer Object (DTO), có nhiệm vụ định nghĩa và kiểm tra dữ liệu đầu vào mà Frontend gửi đến Backend khi đăng nhập bằng Google.

export class GoogleLoginDto {
  @IsString()
  @IsNotEmpty()
  idToken!: string;
}
