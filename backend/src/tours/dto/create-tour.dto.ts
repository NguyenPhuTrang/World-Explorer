
import { Transform, Type } from 'class-transformer';
import {
  ArrayMaxSize,
  IsArray,
  IsEnum,
  IsInt,
  IsOptional,
  IsString,
  Matches,
  MaxLength,
  Min,
  MinLength,
  ValidateIf,
} from 'class-validator';

import {
  TourDifficulty,
  TourStatus,
} from '../../generated/prisma/client.js';

export class CreateTourDto {
  @Transform(({ value }) =>
    typeof value === 'string' ? value.trim() : value,
  )
  @IsString()
  @MinLength(2)
  @MaxLength(200)
  title!: string;

  @Transform(({ value }) =>
    typeof value === 'string' ? value.trim() : value,
  )
  @IsString()
  @Matches(/^[a-z0-9]+(?:-[a-z0-9]+)*$/, {
    message: 'Slug không hợp lệ',
  })
  @MaxLength(220)
  slug!: string;

  @Transform(({ value }) =>
    typeof value === 'string' ? value.trim() : value,
  )
  @IsString()
  @MinLength(10)
  @MaxLength(500)
  summary!: string;

  @Transform(({ value }) =>
    typeof value === 'string' ? value.trim() : value,
  )
  @IsString()
  @MinLength(10)
  @MaxLength(10000)
  description!: string;

  @Type(() => Number)
  @IsInt()
  @Min(1)
  durationDays!: number;

  @Type(() => Number)
  @IsInt()
  @Min(0)
  durationNights!: number;

  @IsOptional()
  @IsEnum(TourDifficulty)
  difficulty?: TourDifficulty;

  @IsOptional()
  @IsEnum(TourStatus)
  status?: TourStatus;

  @Type(() => Number)
  @IsInt()
  @Min(1)
  maxGuestsPerBooking!: number;

  @ValidateIf((_object, value) => value !== undefined)
  @IsArray()
  @ArrayMaxSize(50)
  @IsString({ each: true })
  includedServices?: string[];

  @ValidateIf((_object, value) => value !== undefined)
  @IsArray()
  @ArrayMaxSize(50)
  @IsString({ each: true })
  excludedServices?: string[];

  @IsOptional()
  @IsString()
  @MaxLength(5000)
  cancellationPolicy?: string | null;
}
