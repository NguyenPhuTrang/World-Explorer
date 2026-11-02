import {
  BadRequestException,
  ConflictException,
  Injectable,
  NotFoundException,
} from '@nestjs/common';

import { Prisma } from '../generated/prisma/client.js';
import { PrismaService } from '../prisma/prisma.service.js';

import { CreateDestinationDto } from './dto/create-destination.dto.js';
import { UpdateDestinationDto } from './dto/update-destination.dto.js';
import { QueryDestinationsDto } from './dto/query-destinations.dto.js';

@Injectable()
export class DestinationsService {
  constructor(private readonly prisma: PrismaService) {}

  // 1. Lấy danh sách địa điểm
  async getDestinations(query: QueryDestinationsDto) {
    const {
      page = 1,
      limit = 10,
      search,
      countryId,
      parentId,
      type,
      isActive,
    } = query;

    const where: Prisma.DestinationWhereInput = {
      ...(search && {
        OR: [
          {
            name: {
              contains: search,
              mode: 'insensitive',  // không phân biệt chữ hoa chữ thường
            },
          },
          {
            description: {
              contains: search,
              mode: 'insensitive',
            },
          },
        ],
      }),

      ...(countryId && { countryId }),
      ...(parentId && { parentId }),
      ...(type && { type }),
      ...(isActive !== undefined && { isActive }),
    };

    const [destinations, total] = await Promise.all([
      this.prisma.destination.findMany({
        where,
        skip: (page - 1) * limit,
        take: limit,
        orderBy: {
          createdAt: 'desc',
        },
        include: {
          country: {
            select: {
              id: true,
              name: true,
              code: true,
            },
          },
          parent: {
            select: {
              id: true,
              name: true,
              slug: true,
            },
          },
        },
      }),

      this.prisma.destination.count({ where }),
    ]);

    return {
      data: destinations,
      meta: {
        total,
        page,
        limit,
        totalPages: Math.ceil(total / limit),
      },
    };
  }

  // 2. Lấy chi tiết địa điểm
  async getDestinationById(id: string) {
    const destination = await this.prisma.destination.findUnique({
      where: { id },
      include: {
        country: {
          select: {
            id: true,
            name: true,
            code: true,
          },
        },
        parent: {
          select: {
            id: true,
            name: true,
            slug: true,
            type: true,
          },
        },
        children: {
          select: {
            id: true,
            name: true,
            slug: true,
            type: true,
          },
        },
      },
    });

    if (!destination) {
      throw new NotFoundException('Destination not found');
    }

    return destination;
  }

  // 3. Tạo địa điểm mới
  async createDestination(dto: CreateDestinationDto) {
    // Kiểm tra quốc gia có tồn tại và đang hoạt động
    const country = await this.prisma.country.findFirst({
      where: {
        id: dto.countryId,
        isActive: true,
      },
    });

    if (!country) {
      throw new BadRequestException(
        'Country does not exist or is inactive',
      );
    }

    // Kiểm tra địa điểm cha nếu có
    if (dto.parentId) {
      await this.validateParent(
        dto.parentId,
        dto.countryId,
      );
    }

    try {
      return await this.prisma.destination.create({
        data: dto,
        include: {
          country: {
            select: {
              id: true,
              name: true,
              code: true,
            },
          },
          parent: {
            select: {
              id: true,
              name: true,
              slug: true,
            },
          },
        },
      });
    } catch (error) {
      this.handlePrismaError(error);
    }
  }

  // 4. Cập nhật địa điểm
  async updateDestination(
    id: string,
    dto: UpdateDestinationDto,
  ) {
    const existingDestination =
      await this.prisma.destination.findUnique({
        where: { id },
      });

    if (!existingDestination) {
      throw new NotFoundException('Destination not found');
    }

    if (Object.keys(dto).length === 0) {
      throw new BadRequestException(
        'Please provide at least one field to update',
      );
    }

    // Xác định countryId sau khi cập nhật
    const nextCountryId =
      dto.countryId ?? existingDestination.countryId;

    // Kiểm tra quốc gia nếu countryId được thay đổi
    if (dto.countryId) {
      const country = await this.prisma.country.findFirst({
        where: {
          id: dto.countryId,
          isActive: true,
        },
      });

      if (!country) {
        throw new BadRequestException(
          'Country does not exist or is inactive',
        );
      }
    }

    // Xác định parentId sau khi cập nhật
    const nextParentId =
      dto.parentId !== undefined
        ? dto.parentId
        : existingDestination.parentId;

    // Kiểm tra quan hệ cha-con
    if (nextParentId) {
      await this.validateParent(
        nextParentId,
        nextCountryId,
        id,
      );
    }

    try {
      return await this.prisma.destination.update({
        where: { id },
        data: dto,
        include: {
          country: {
            select: {
              id: true,
              name: true,
              code: true,
            },
          },
          parent: {
            select: {
              id: true,
              name: true,
              slug: true,
            },
          },
        },
      });
    } catch (error) {
      this.handlePrismaError(error);
    }
  }

  // 5. Xóa mềm địa điểm
  async deleteDestination(id: string) {
    const destination =
      await this.prisma.destination.findUnique({
        where: { id },
      });

    if (!destination) {
      throw new NotFoundException('Destination not found');
    }

    if (!destination.isActive) {
      throw new BadRequestException(
        'Destination is already inactive',
      );
    }

    return this.prisma.destination.update({
      where: { id },
      data: {
        isActive: false,
      },
    });
  }

  // Kiểm tra địa điểm cha
  private async validateParent(
    parentId: string,
    countryId: string,
    currentDestinationId?: string,
  ) {
    if (parentId === currentDestinationId) {
      throw new BadRequestException(
        'A destination cannot be its own parent',
      );
    }

    const parent = await this.prisma.destination.findFirst({
      where: {
        id: parentId,
        isActive: true,
      },
    });

    if (!parent) {
      throw new BadRequestException(
        'Parent destination does not exist or is inactive',
      );
    }

    if (parent.countryId !== countryId) {
      throw new BadRequestException(
        'Parent destination must belong to the same country',
      );
    }

    // Không cho phép tạo vòng lặp trong cây địa điểm
    let ancestorId: string | null = parent.parentId;

    while (ancestorId) {
      if (ancestorId === currentDestinationId) {
        throw new BadRequestException(
          'Invalid parent: circular destination hierarchy',
        );
      }

      const ancestor =
        await this.prisma.destination.findUnique({
          where: { id: ancestorId },
          select: { parentId: true },
        });

      ancestorId = ancestor?.parentId ?? null;
    }
  }

  // Xử lý lỗi Prisma
  private handlePrismaError(error: unknown): never {
    if (
      error instanceof Prisma.PrismaClientKnownRequestError
    ) {
      if (error.code === 'P2002') {
        throw new ConflictException(
          'Destination slug or another unique field already exists',
        );
      }

      if (error.code === 'P2003') {
        throw new BadRequestException(
          'Invalid relation reference',
        );
      }
    }

    throw error;
  }
}