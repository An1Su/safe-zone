# Seed Images Directory

This directory contains placeholder images for seed data products.

## How It Works

1. Place product images here with filenames matching product IDs (e.g., `{productId}.png`)
2. The `DataInitializer` will automatically copy these images to the uploads directory when seeding
3. If an image doesn't exist for a product, a minimal placeholder PNG will be created

## Image Requirements

- Format: PNG, JPEG, or WebP
- Size: Recommended max 2MB (matches MediaService limits)
- Naming: Use product ID as filename (e.g., `507f1f77bcf86cd799439011.png`)

## Adding Real Images

To use real product images:

1. Get the product IDs after products are created (check logs or database)
2. Rename your image files to match product IDs
3. Place them in this directory
4. Restart the media service or run the seed script again

## Example

```
seed-images/
├── 507f1f77bcf86cd799439011.png  (Velvet Matte Lipstick)
├── 507f1f77bcf86cd799439012.png  (Rose Gold Eyeshadow Palette)
└── ...
```
