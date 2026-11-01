import { Body, Controller, Get, Param, Patch, Query, Req, UseGuards } from '@nestjs/common';
import { JwtAuthGuard } from '../auth/guards/jwt-auth.guard.js';
import { UsersService } from './users.service.js';
import { UpdateProfileDto } from './dto/update-profile.dto.js';
import { Roles } from '../auth/decorators/roles.decorator.js';
import { RolesGuard } from '../auth/guards/roles.guard.js';
import { QueryUsersDto } from './dto/query-users.dto.js';
import { UpdateUserStatusDto } from './dto/update-user-status.dto.js';


interface AuthenticatedUser {
    userId: string;
    role: string
}

type AuthenticatedRequest = Request & {
    user: AuthenticatedUser;
};

@Controller('users')
@UseGuards(JwtAuthGuard)
export class UsersController {

    constructor(  // constructor() Đây là hàm khởi tạo của một class trong TypeScript. Nó được gọi khi một instance của class được tạo ra.
        private readonly usersService: UsersService, // Đây là cách TypeScript vừa khai báo thuộc tính cho class, vừa khai báo tham số cho constructor.
    ) {} // phần định nghĩa của contructer Dấu {} có nghĩa là constructor không cần thực hiện thêm câu lệnh nào bên trong.
 
    //1 Get current user Profile
    @Get('me')
    getProfile(@Req() req: AuthenticatedRequest) {
        return this.usersService.getProfile(
            req.user.userId,
        )
    }

    //2 Update current user profile
    @Patch('me')
    updateProfile(
        @Req() req: AuthenticatedRequest,
        @Body() dto: UpdateProfileDto,
     ) {
        return this.usersService.updateProfile(
            req.user.userId, 
            dto
        )
     }

     //3 Get users (Admin only)
     @Get('id')
     @Roles('ADMIN')
     @UseGuards(RolesGuard)
     getUsers(@Query() query: QueryUsersDto) {
        return this.usersService.getUsers(query);
     }

     //4 Get user by Id (Admin only)
     @Get('me')
     @Roles('ADMIN')
     @UseGuards(RolesGuard)
     getUserById(@Param('id') id: string) {
        return this.usersService.getUserById(id);
     }

     //5 Update user status (Admi only)
     @Patch(':id/status')
     @Roles('ADMIN')
     @UseGuards(RolesGuard)
     updateUserStatus(
        @Param('id') id: string,
        @Req() req: AuthenticatedRequest,
        @Body() dto: UpdateUserStatusDto
     ) {
        return this.usersService.updateUserStatus(
            id,
            req.user.userId,
            dto,
        )
     }
}
