
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

import { DestinationsService } from './destinations.service.js';
import { CreateDestinationDto } from './dto/create-destination.dto.js';
import { UpdateDestinationDto } from './dto/update-destination.dto.js';
import { QueryDestinationsDto } from './dto/query-destinations.dto.js';

import { JwtAuthGuard } from '../auth/guards/jwt-auth.guard.js';
import { RolesGuard } from '../auth/guards/roles.guard.js';
import { Roles } from '../auth/decorators/roles.decorator.js';

@Controller('destinations')
export class DestinationsController {
  constructor(
    private readonly destinationsService: DestinationsService,
  ) {}

  // GET /api/v1/destinations
  @Get()
  getDestinations(@Query() query: QueryDestinationsDto) {
    return this.destinationsService.getDestinations(query);
  }

  // GET /api/v1/destinations/:id
  @Get(':id')
  getDestinationById(
    @Param('id', ParseUUIDPipe) id: string,
  ) {
    return this.destinationsService.getDestinationById(id);
  }

  // POST /api/v1/destinations
  @Post()
  @UseGuards(JwtAuthGuard, RolesGuard)
  @Roles('ADMIN')
  createDestination(@Body() dto: CreateDestinationDto) {
    return this.destinationsService.createDestination(dto);
  }

  // PATCH /api/v1/destinations/:id
  @Patch(':id')
  @UseGuards(JwtAuthGuard, RolesGuard)
  @Roles('ADMIN')
  updateDestination(
    @Param('id', ParseUUIDPipe) id: string,
    @Body() dto: UpdateDestinationDto,
  ) {
    return this.destinationsService.updateDestination(id, dto);
  }

  // DELETE /api/v1/destinations/:id
  @Delete(':id')
  @UseGuards(JwtAuthGuard, RolesGuard)
  @Roles('ADMIN')
  deleteDestination(
    @Param('id', ParseUUIDPipe) id: string,
  ) {
    return this.destinationsService.deleteDestination(id);
  }
}
