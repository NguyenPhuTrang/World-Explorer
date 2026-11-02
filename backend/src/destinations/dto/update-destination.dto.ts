import { PartialType } from '@nestjs/mapped-types';
import { CreateDestinationDto } from './create-destination.dto.js';

export class UpdateDestinationDto extends PartialType( // PartialType chuyển tất cả thuộc tính trong CreateDestinationDto thành optional.
  CreateDestinationDto,
) {}