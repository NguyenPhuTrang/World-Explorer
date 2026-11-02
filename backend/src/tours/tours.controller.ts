
import {
  Body,
  Controller,
  Delete,
  Get,
  Param,
  ParseUUIDPipe,
  Patch,
  Post,
  Query,
  UseGuards,
} from '@nestjs/common';

import { ToursService } from './tours.service.js';

import { CreateTourDto } from './dto/create-tour.dto.js';
import { UpdateTourDto } from './dto/update-tour.dto.js';
import { QueryToursDto } from './dto/query-tours.dto.js';

import { JwtAuthGuard } from '../auth/guards/jwt-auth.guard.js';
import { RolesGuard } from '../auth/guards/roles.guard.js';
import { Roles } from '../auth/decorators/roles.decorator.js';

@Controller('tours')
export class ToursController {
  constructor(
    private readonly toursService: ToursService,
  ) {}

  // 1. Lấy danh sách Tour
  @Get()
  getTours(@Query() query: QueryToursDto) {
    return this.toursService.getTours(query);
  }

  // 2. Lấy chi tiết Tour
  @Get(':id')
  getTourById(
    @Param('id', ParseUUIDPipe) id: string,
  ) {
    return this.toursService.getTourById(id);
  }

  // 3. Tạo Tour mới - ADMIN
  @Post()
  @UseGuards(JwtAuthGuard, RolesGuard)
  @Roles('ADMIN')
  createTour(@Body() dto: CreateTourDto) {
    return this.toursService.createTour(dto);
  }

  // 4. Cập nhật Tour - ADMIN
  @Patch(':id')
  @UseGuards(JwtAuthGuard, RolesGuard)
  @Roles('ADMIN')
  updateTour(
    @Param('id', ParseUUIDPipe) id: string,
    @Body() dto: UpdateTourDto,
  ) {
    return this.toursService.updateTour(id, dto);
  }

  // 5. Xóa mềm Tour - ADMIN
  @Delete(':id')
  @UseGuards(JwtAuthGuard, RolesGuard)
  @Roles('ADMIN')
  deleteTour(
    @Param('id', ParseUUIDPipe) id: string,
  ) {
    return this.toursService.deleteTour(id);
  }
}
