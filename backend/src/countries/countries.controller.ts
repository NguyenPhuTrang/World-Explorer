import {
  Body,
  Delete,
  Get,
  Param,
  Patch,
  Post,
  UseGuards,
} from '@nestjs/common';

import { Controller, Query } from '@nestjs/common';
import { CountriesService } from './countries.service.js';
import { QueryCountriesDto } from './dto/query-countries.dto.js';
import { JwtAuthGuard } from '../auth/guards/jwt-auth.guard.js';
import { RolesGuard } from '../auth/guards/roles.guard.js';
import { Roles } from '../auth/decorators/roles.decorator.js';
import { CreateCountryDto } from './dto/create-country.dto.js';
import { UpdateCountryDto } from './dto/update-country.dto.js';

@Controller('countries')
export class CountriesController {
    constructor(private readonly countriesService: CountriesService) {}
    
    // 1. Get all countries
    @Get()
    getCountries(@Query() query: QueryCountriesDto) {
        return this.countriesService.getCountries(query)
    }

    // 2 Get coutry by ID
    @Get(':id')
    getCountryById(@Param('id') id: string) {
        return this.countriesService.getCountryById(id);
    }

    // 3 Create country (Admin only)
    @Post()
    @UseGuards(JwtAuthGuard, RolesGuard)
    @Roles('ADMIN')
    createCountry(@Body() dto: CreateCountryDto){
        return this.countriesService.createCountry(dto)
    }

    // 4 Update country (ADMIN only)
    @Patch(':id')
    @UseGuards(JwtAuthGuard, RolesGuard)
    @Roles('ADMIN')
    updateCountry(
        @Param('id') id: string,
            @Body() dto: UpdateCountryDto,
        ) {
            return this.countriesService.updateCountry(id, dto);
        }
    
    // 5. Delete country (ADMIN only)
    @Delete(':id')
    @UseGuards(JwtAuthGuard, RolesGuard)
    @Roles('ADMIN')
    deleteCountry(@Param('id') id: string) {
        return this.countriesService.deleteCountry(id);
    }
}
