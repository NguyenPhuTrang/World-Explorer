// kết nối và quản lý các thành phần thuộc Country Module
import { Module } from '@nestjs/common';
import { Prisma } from '../generated/prisma/client.js';
import { PrismaModule } from '../prisma/prisma.module.js';
import { CountriesController } from './countries.controller.js';
import { CountriesService } from './countries.service.js';

@Module({
    imports: [PrismaModule],
    controllers: [CountriesController],
    providers: [CountriesService],
    exports: [CountriesController]
})
export class CountriesModule {}
