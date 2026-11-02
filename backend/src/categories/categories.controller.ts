import { Body, Controller, Delete, Get, Param, ParseUUIDPipe, Patch, Post, Query, UseGuards } from '@nestjs/common';
import { CategoriesService } from './categories.service.js';
import { QueryCategoriesDto } from './dto/query-categories.dto.js';
import { JwtAuthGuard } from '../auth/guards/jwt-auth.guard.js';
import { RolesGuard } from '../auth/guards/roles.guard.js';
import { Roles } from '../auth/decorators/roles.decorator.js';
import { CreateCategoryDto } from './dto/create-category.dto.js';
import { UpdateCategoryDto } from './dto/update-category.dto.js';

@Controller('categories')
export class CategoriesController {
    constructor(private readonly categoriesService: CategoriesService){}

    // 1. Get categories
    @Get()
    getCategories(@Query() query: QueryCategoriesDto){
        return this.categoriesService.getCategories(query)
    } 

    // 2. Get Category by ID
    @Get(':id')
    getCategoryById(
        @Param('id', ParseUUIDPipe) id: string // kiem tra gia tri id co dinh dang UUID khong
    ){
        return this,this.categoriesService.getCategoryById(id)
    }

    // 3. Create
    @Post()
    @UseGuards(JwtAuthGuard, RolesGuard)
    @Roles('ADMIN')
    createCategory(@Body() dto: CreateCategoryDto){
        return this.categoriesService.createCategory(dto)
    }

    // 4. Update
    @Patch(':id')
    @UseGuards(JwtAuthGuard, RolesGuard)
    @Roles('ADMIN')
    updateCategory(
        @Param('id', ParseUUIDPipe) id: string,
        @Body() dto: UpdateCategoryDto,
    ){
        return this.categoriesService.updateCategory(id, dto)
    }

    // 5. Soft delete 
    @Delete(':id')
    @UseGuards(JwtAuthGuard, RolesGuard) // JwtAuthGuard kiem tra xem nguoi dung da dang nhap chua
    @Roles('ADMIN')
    deleteCategory(
        @Param('id', ParseUUIDPipe) id: string,
    ){
        this.categoriesService.deleteCategory(id)
    }
     
}
