// Guard này chịu trách nhiệm yêu cầu người dùng phải có Access Token hợp lệ.
import { Injectable } from '@nestjs/common';
import { AuthGuard } from '@nestjs/passport';

@Injectable()
export class JwtAuthGuard extends AuthGuard('jwt') {}