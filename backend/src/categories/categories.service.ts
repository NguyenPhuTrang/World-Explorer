
import {
  BadRequestException,
  Injectable,
  NotFoundException,
} from '@nestjs/common';

import { Prisma } from '../generated/prisma/client.js';
import { PrismaService } from '../prisma/prisma.service.js';

import { CreateCategoryDto } from './dto/create-category.dto.js';
import { UpdateCategoryDto } from './dto/update-category.dto.js';
import { QueryCategoriesDto } from './dto/query-categories.dto.js';

@Injectable()
export class CategoriesService {
  constructor(private readonly prisma: PrismaService) {}

  // 1. Lấy danh sách Category
  async getCategories(query: QueryCategoriesDto) {
    const {
      page = 1,
      limit = 10,
      search,
      parentId, // nếu truyền vào query thì có nghĩa là lấy danh mục con của của danh mục đó , không truyển thì lấy tất cả
      isActive,
    } = query;

    const where: Prisma.CategoryWhereInput = {
      ...(search && {
        OR: [
          {
            name: {
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

      ...(parentId && { parentId }),
      ...(isActive !== undefined && { isActive }),
    };

    const [categories, total] = await Promise.all([
      this.prisma.category.findMany({
        where,
        skip: (page - 1) * limit,
        take: limit,
        orderBy: {
          createdAt: 'desc',
        },
        include: {
          parent: {
            select: {
              id: true,
              name: true,
              slug: true,
            },
          },
          _count: {
            select: {
              children: true,
              tourCategories: true,
            },
          },
        },
      }),

      this.prisma.category.count({ where }),
    ]);

    return {
      data: categories,
      meta: {
        total,
        page,
        limit,
        totalPages: Math.ceil(total / limit),
      },
    };
  }

  // 2. Lấy chi tiết Category
  async getCategoryById(id: string) {
    const category = await this.prisma.category.findUnique({
      where: { id },
      include: {
        parent: {
          select: {
            id: true,
            name: true,
            slug: true,
          },
        },
        children: {
          select: {
            id: true,
            name: true,
            slug: true,
            isActive: true,
          },
        },
        _count: {
          select: {
            children: true,
            tourCategories: true,
          },
        },
      },
    });

    if (!category) {
      throw new NotFoundException('Category not found');
    }

    return category;
  }

  // 3. Tạo Category mới
  async createCategory(dto: CreateCategoryDto) {
    // Kiểm tra danh mục cha nếu có
    if (dto.parentId) {
      await this.validateParent(dto.parentId);
    }

    return this.prisma.category.create({
      data: dto,
      include: {
        parent: {
          select: {
            id: true,
            name: true,
            slug: true,
          },
        },
      },
    });
  }

  // 4. Cập nhật Category
  async updateCategory(id: string, dto: UpdateCategoryDto) {
    const existingCategory = await this.prisma.category.findUnique({
      where: { id },
    });

    if (!existingCategory) {
      throw new NotFoundException('Category not found');
    }

    if (Object.keys(dto).length === 0) {
      throw new BadRequestException(
        'Please provide at least one field to update',
      );
    }

    // Xác định parentId sau khi cập nhật
    const nextParentId =
      dto.parentId !== undefined
        ? dto.parentId
        : existingCategory.parentId;

    // Kiểm tra quan hệ cha-con
    if (nextParentId) {
      await this.validateParent(nextParentId, id);
    }

    return this.prisma.category.update({
      where: { id },
      data: dto,
      include: {
        parent: {
          select: {
            id: true,
            name: true,
            slug: true,
          },
        },
      },
    });
  }

  // 5. Xóa mềm Category
  async deleteCategory(id: string) {
    const category = await this.prisma.category.findUnique({
      where: { id },
    });

    if (!category) {
      throw new NotFoundException('Category not found');
    }

    if (!category.isActive) {
      throw new BadRequestException(
        'Category is already inactive',
      );
    }

    return this.prisma.category.update({
      where: { id },
      data: {
        isActive: false,
      },
    });
  }

  // Kiểm tra danh mục cha
  private async validateParent(
    parentId: string,
    currentCategoryId?: string,
  ) {
    if (parentId === currentCategoryId) {
      throw new BadRequestException(
        'A category cannot be its own parent',
      );
    }

    const parent = await this.prisma.category.findFirst({
      where: {
        id: parentId,
        isActive: true,
      },
    });

    if (!parent) {
      throw new BadRequestException(
        'Parent category does not exist or is inactive',
      );
    }

    // Kiểm tra vòng lặp trong cấu trúc cây
    let ancestorId: string | null = parent.parentId;

    while (ancestorId) {
      if (ancestorId === currentCategoryId) {
        throw new BadRequestException(
          'Invalid parent: circular category hierarchy',
        );
      }

      const ancestor = await this.prisma.category.findUnique({
        where: { id: ancestorId },
        select: { parentId: true },
      });

      ancestorId = ancestor?.parentId ?? null;
    }
  }
}
