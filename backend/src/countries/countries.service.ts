import { BadRequestException, ConflictException, Injectable, NotFoundException } from '@nestjs/common';
import { PrismaService } from '../prisma/prisma.service.js';
import { QueryCountriesDto } from './dto/query-countries.dto.js';
import { Prisma } from '../generated/prisma/client.js';
import { CreateCountryDto } from './dto/create-country.dto.js';
import { UpdateCountryDto } from './dto/update-country.dto.js';

@Injectable()
export class CountriesService {
    constructor(private readonly prisma: PrismaService) {} // Constructor này giúp NestJS tự động cung cấp những dependency mà Service cần sử dụng, thay vì chúng ta phải tự khởi tạo chúng.

    // 1 Get countries with pagination, search and filter
    async getCountries(query: QueryCountriesDto){
        const {
            page = 1,
            limit = 10,
            search,
            isActive
        } = query;

        const skip = (page -1) * limit;
        const keyword = search?.trim();

        const where: Prisma.CountryWhereInput = {
            ...(keyword 
                ? {
                    name: {
                        contains : keyword,
                        mode: 'insensitive'
                    }
                }
                    :{}),
            ...(isActive !== undefined  // ... (spread) là có tác dụng đổ nội dung của object đó vào object bên ngoài (where), Điều kiện chỉ trả về một trong hai object, rồi object đó được đổ vào where
                ? {isActive}
                    : {}),
                    // where = { isActive: true } → chỉ lấy quốc gia đang hoạt động
                    // where = {}                 → lấy tất cả
        }

        const [countries, total] = await this.prisma.$transaction([
            this.prisma.country.findMany({
                where,
                skip,
                take: limit,
                orderBy: {
                    name: 'asc'
                }
            }),
            this.prisma.country.count({where}) // đếm xem có bao nhiêu quốc gia thảo mãn điều kiện where
        ]);

        return {
            data: countries,
            pagination: {
                page,
                limit,
                total,
                totalPages: Math.ceil(total/limit) // Math.ceil làm tròn đến số nguyên gần nhất
            }
        }
    }

   // 2. Get country by ID
  async getCountryById(id: string) {
    const country = await this.prisma.country.findUnique({
      where: { id },
    });

    if (!country) {
      throw new NotFoundException(
        'Không tìm thấy quốc gia',
      );
    }

    return country;
  }

  // 3. Create country
  async createCountry(dto: CreateCountryDto) {
    try {
      return await this.prisma.country.create({
        data: {
          name: dto.name,
          code: dto.code,
          slug: dto.slug,
          description: dto.description,
          imageUrl: dto.imageUrl,
        },
      });
    } catch (error) {
      if (
        error instanceof Prisma.PrismaClientKnownRequestError &&
        error.code === 'P2002'
      ) {
        throw new ConflictException(
          'Mã quốc gia hoặc slug đã tồn tại',
        );
      }

      throw error;
    }
  }

  // 4. Update country
  async updateCountry(
    id: string,
    dto: UpdateCountryDto,
  ) {
    const existingCountry = await this.prisma.country.findUnique({
      where: { id },
      select: { id: true },
    });

    if (!existingCountry) {
      throw new NotFoundException(
        'Không tìm thấy quốc gia',
      );
    }

    if (Object.keys(dto).length === 0) {
      throw new BadRequestException(
        'Vui lòng cung cấp ít nhất một thông tin cần cập nhật',
      );
    }

    try {
      return await this.prisma.country.update({
        where: { id },
        data: dto,
      });
    } catch (error) {
      if (
        error instanceof Prisma.PrismaClientKnownRequestError &&
        error.code === 'P2002'
      ) {
        throw new ConflictException(
          'Mã quốc gia hoặc slug đã tồn tại',
        );
      }

      throw error;
    }
  }

  // 5. Soft delete country
  async deleteCountry(id: string) {
    const existingCountry = await this.prisma.country.findUnique({
      where: { id },
      select: {
        id: true,
        isActive: true,
      },
    });

    if (!existingCountry) {
      throw new NotFoundException(
        'Không tìm thấy quốc gia',
      );
    }

    if (!existingCountry.isActive) {
      throw new BadRequestException(
        'Quốc gia này đã được vô hiệu hóa',
      );
    }

    return this.prisma.country.update({
      where: { id },
      data: {
        isActive: false,
      },
    });
  }
}