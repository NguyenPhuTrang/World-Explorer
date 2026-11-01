import { BadRequestException, ForbiddenException, Injectable, NotAcceptableException, NotFoundException } from '@nestjs/common';
import { PrismaService } from '../prisma/prisma.service.js';
import { UpdateProfileDto } from './dto/update-profile.dto.js';
import { QueryUsersDto } from './dto/query-users.dto.js';
import { UpdateUserStatusDto } from './dto/update-user-status.dto.js';

@Injectable()
export class UsersService {
    constructor(private readonly prisma: PrismaService) {}

    // 1. Get current user profile
    async getProfile(userId: string) {
        const user = await this.prisma.user.findFirst({
            where: {
                id: userId,
                isActive: true,
                deletedAt: null,
            },
            select: {
                id: true,
                email: true,
                fullName: true,
                avatarUrl: true,
                isActive: true,
                createdAt: true,
                updatedAt: true,
                role: {
                    select: {
                        name: true,
                    }
                }
            }
        });

        if(!user){
            throw new NotAcceptableException("Không tìm thấy tài khoản");
        }

        return user;
    }

    // 2. Update current user's Profile

    async updateProfile(userId: string, dto: UpdateProfileDto) {
        const data: {
            fullName?: string,
            avatarUrl?: string | null;
        } = {}; // Nếu ban đầu không có dữ liệu thì trả về 1 object rỗng sau đó chúng ta mới cập nhật dữ liệu

        if (dto.fullName !== undefined) {
        data.fullName = dto.fullName.trim();
        }

        if (dto.avatarUrl !== undefined) {
        data.avatarUrl = dto.avatarUrl;
        }

        if (Object.keys(data).length === 0) {
            throw new BadRequestException(
                'Vui lòng cung cấp ít nhất một thông tin cần cập nhật',
            );
        }

        const existingUser = await this.prisma.user.findFirst({
      where: {
        id: userId,
        isActive: true,
        deletedAt: null,
      },
      select: {
        id: true,
      },
    });

    if (!existingUser) {
      throw new NotFoundException('Không tìm thấy tài khoản');
    }

    const updatedUser = await this.prisma.user.update({
      where: {
        id: userId,
      },
      data,
      select: {
        id: true,
        email: true,
        fullName: true,
        avatarUrl: true,
        isActive: true,
        updatedAt: true,
        role: {
          select: {
            name: true,
          },
        },
      },
    });

    return updatedUser;
  }

  // 3. Get users with pagination and search
  async getUsers(query: QueryUsersDto) {
    const { page = 1, limit = 10, search } = query;

    const skip = (page - 1) * limit;
    const keyword = search?.trim();

    const where = {
      deletedAt: null,
      ...(keyword
        ? {
            OR: [
              {
                fullName: {
                  contains: keyword,
                  mode: 'insensitive' as const,
                },
              },
              {
                email: {
                  contains: keyword,
                  mode: 'insensitive' as const,
                },
              },
            ],
          }
        : {}),
    };

    const [users, total] = await this.prisma.$transaction([
      this.prisma.user.findMany({
        where,
        skip,
        take: limit,
        orderBy: {
          createdAt: 'desc',
        },
        select: {
          id: true,
          email: true,
          fullName: true,
          avatarUrl: true,
          isActive: true,
          createdAt: true,
          role: {
            select: {
              name: true,
            },
          },
        },
      }),
      this.prisma.user.count({ where }),
    ]);

    return {
      data: users,
      pagination: {
        page,
        limit,
        total,
        totalPages: Math.ceil(total / limit),
      },
    };
  }

  // 4. Get user by ID
  async getUserById(userId: string) {
    const user = await this.prisma.user.findFirst({
      where: {
        id: userId,
        deletedAt: null,
      },
      select: {
        id: true,
        email: true,
        fullName: true,
        avatarUrl: true,
        isActive: true,
        createdAt: true,
        updatedAt: true,
        role: {
          select: {
            name: true,
          },
        },
      },
    });

    if (!user) {
      throw new NotFoundException('Không tìm thấy người dùng');
    }

    return user;
  }

  // 5. Update user status (Admin only - authorization handled separately)
  async updateUserStatus(
    userId: string,
    adminId: string,
    dto: UpdateUserStatusDto,
  ) {
    if (userId === adminId) {
      throw new ForbiddenException(
        'Bạn không thể tự khóa hoặc thay đổi trạng thái tài khoản của mình',
      );
    }

    const existingUser = await this.prisma.user.findFirst({
      where: {
        id: userId,
        deletedAt: null,
      },
      select: {
        id: true,
      },
    });

    if (!existingUser) {
      throw new NotFoundException('Không tìm thấy người dùng');
    }

    const updatedUser = await this.prisma.user.update({
      where: {
        id: userId,
      },
      data: {
        isActive: dto.isActive,
      },
      select: {
        id: true,
        email: true,
        fullName: true,
        isActive: true,
        updatedAt: true,
        role: {
          select: {
            name: true,
          },
        },
      },
    });

    return updatedUser;
  }
}
