
import {
  BadRequestException,
  ConflictException,
  Injectable,
  NotFoundException,
} from '@nestjs/common';

import { Prisma } from '../generated/prisma/client.js';
import { PrismaService } from '../prisma/prisma.service.js';

import { CreateTourDto } from './dto/create-tour.dto.js';
import { UpdateTourDto } from './dto/update-tour.dto.js';
import { QueryToursDto } from './dto/query-tours.dto.js';

@Injectable()
export class ToursService {
  constructor(private readonly prisma: PrismaService) {}

  // 1. Lấy danh sách Tour
  async getTours(query: QueryToursDto) {
    const {
      page = 1,
      limit = 10,
      search,
      difficulty,
      status,
    } = query;

    const where: Prisma.TourWhereInput = { //TourWhereInput là một kiểu dữ liệu (type) do Prisma tự sinh ra, mô tả hình dạng của điều kiện lọc where cho model Tour.
      deletedAt: null, //chỉ lấy những bản ghi chưa bị xóa mềm.

      ...(search && {
        OR: [
          {
            title: {
              contains: search,
              mode: 'insensitive',
            },
          },
          {
            summary: {
              contains: search,
              mode: 'insensitive',
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

      ...(difficulty && { difficulty }),
      ...(status && { status }),
    };

    const [tours, total] = await Promise.all([ // phan trang tra ve cac du lieu
      this.prisma.tour.findMany({
        where,
        skip: (page - 1) * limit,
        take: limit,
        orderBy: {
          createdAt: 'desc',
        },
        include: {
          _count: {
            select: {
              tourDestinations: true,
              tourCategories: true,
              images: true,
              departures: true,
              reviews: true,
              favorites: true,
            },
          },
        },
      }),

      this.prisma.tour.count({ where }),
    ]);

    return {
      data: tours,
      meta: {
        total,
        page,
        limit,
        totalPages: Math.ceil(total / limit),
      },
    };
  }

  // 2. Lấy chi tiết Tour
  async getTourById(id: string) {
    const tour = await this.prisma.tour.findFirst({
      where: {
        id,
        deletedAt: null,
      },
      include: {
        _count: {
          select: {
            tourDestinations: true,
            tourCategories: true,
            images: true,
            itineraries: true,
            departures: true,
            reviews: true,
            favorites: true,
          },
        },
      },
    });

    if (!tour) {
      throw new NotFoundException('Tour not found');
    }

    return tour;
  }

  // 3. Tạo Tour mới
  async createTour(dto: CreateTourDto) {
    if (dto.durationNights > dto.durationDays) {
      throw new BadRequestException(
        'Duration nights cannot exceed duration days',
      );
    }

    try {
      return await this.prisma.tour.create({
        data: {
          title: dto.title,
          slug: dto.slug,
          summary: dto.summary,
          description: dto.description,
          durationDays: dto.durationDays,
          durationNights: dto.durationNights,
          maxGuestsPerBooking: dto.maxGuestsPerBooking,

          ...(dto.difficulty !== undefined && {
            difficulty: dto.difficulty,
          }),

          ...(dto.status !== undefined && {
            status: dto.status,
          }),

          ...(dto.includedServices !== undefined && {
            includedServices:
              dto.includedServices as Prisma.InputJsonValue,
          }),

          ...(dto.excludedServices !== undefined && {
            excludedServices:
              dto.excludedServices as Prisma.InputJsonValue,
          }),

          ...(dto.cancellationPolicy !== undefined && {
            cancellationPolicy: dto.cancellationPolicy,
          }),
        },
      });
    } catch (error) {
      this.handlePrismaError(error);
    }
  }

  // 4. Cập nhật Tour
  async updateTour(id: string, dto: UpdateTourDto) {
    const existingTour = await this.prisma.tour.findFirst({
      where: {
        id,
        deletedAt: null,
      },
    });

    if (!existingTour) {
      throw new NotFoundException('Tour not found');
    }

    if (Object.keys(dto).length === 0) {
      throw new BadRequestException(
        'Please provide at least one field to update',
      );
    }

    const nextDurationDays =
      dto.durationDays ?? existingTour.durationDays;

    const nextDurationNights =
      dto.durationNights ?? existingTour.durationNights;

    if (nextDurationNights > nextDurationDays) {
      throw new BadRequestException(
        'Duration nights cannot exceed duration days',
      );
    }

    try {
      return await this.prisma.tour.update({
        where: { id },
        data: {
          ...(dto.title !== undefined && {
            title: dto.title,
          }),

          ...(dto.slug !== undefined && {
            slug: dto.slug,
          }),

          ...(dto.summary !== undefined && {
            summary: dto.summary,
          }),

          ...(dto.description !== undefined && {
            description: dto.description,
          }),

          ...(dto.durationDays !== undefined && {
            durationDays: dto.durationDays,
          }),

          ...(dto.durationNights !== undefined && {
            durationNights: dto.durationNights,
          }),

          ...(dto.difficulty !== undefined && {
            difficulty: dto.difficulty,
          }),

          ...(dto.status !== undefined && {
            status: dto.status,
          }),

          ...(dto.maxGuestsPerBooking !== undefined && {
            maxGuestsPerBooking: dto.maxGuestsPerBooking,
          }),

          ...(dto.includedServices !== undefined && {
            includedServices:
              dto.includedServices as Prisma.InputJsonValue,
          }),

          ...(dto.excludedServices !== undefined && {
            excludedServices:
              dto.excludedServices as Prisma.InputJsonValue,
          }),

          ...(dto.cancellationPolicy !== undefined && {
            cancellationPolicy: dto.cancellationPolicy,
          }),
        },
      });
    } catch (error) {
      this.handlePrismaError(error);
    }
  }

  // 5. Xóa mềm Tour
  async deleteTour(id: string) {
    const tour = await this.prisma.tour.findFirst({
      where: {
        id,
        deletedAt: null,
      },
    });

    if (!tour) {
      throw new NotFoundException('Tour not found');
    }

    return this.prisma.tour.update({
      where: { id },
      data: {
        deletedAt: new Date(),
      },
    });
  }

  // Xử lý lỗi Prisma
  private handlePrismaError(error: unknown): never {
    if (
      error instanceof Prisma.PrismaClientKnownRequestError
    ) {
      if (error.code === 'P2002') {
        throw new ConflictException(
          'Tour slug already exists',
        );
      }
    }

    throw error;
  }
}
