
import { Transform, Type } from 'class-transformer';

import {
  IsEnum,
  IsInt,
  IsOptional,
  IsString,
  Max,
  MaxLength,
  Min,
} from 'class-validator';

import {
  TourDifficulty,
  TourStatus,
} from '../../generated/prisma/client.js';

export class QueryToursDto {
  @IsOptional()
  @Type(() => Number)
  @IsInt()
  @Min(1)
  page: number = 1;

  @IsOptional()
  @Type(() => Number)
  @IsInt()
  @Min(1)
  @Max(100)
  limit: number = 10;

  @IsOptional()
  @Transform(({ value }) =>
    typeof value === 'string' ? value.trim() : value,
  )
  @IsString()
  @MaxLength(200)
  search?: string;

  @IsOptional()
  @IsEnum(TourDifficulty)
  difficulty?: TourDifficulty;

  @IsOptional()
  @IsEnum(TourStatus)
  status?: TourStatus;
}
