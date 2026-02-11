import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { CartItem } from '../../models/cart.model';
import { Media, Product } from '../../models/ecommerce.model';
import { AuthService } from '../../services/auth.service';
import { CartService } from '../../services/cart.service';
import { MediaService } from '../../services/media.service';
import { ProductService } from '../../services/product.service';
import { ImageSliderComponent } from '../shared/image-slider/image-slider.component';

@Component({
  selector: 'app-product-detail',
  standalone: true,
  imports: [CommonModule, RouterModule, ImageSliderComponent],
  templateUrl: './product-detail.component.html',
  styleUrl: './product-detail.component.scss',
})
export class ProductDetailComponent implements OnInit {
  product: Product | null = null;
  productMedia: Media[] = [];
  loading = true;
  error = '';

  constructor(
    private route: ActivatedRoute,
    private productService: ProductService,
    private mediaService: MediaService,
    private cartService: CartService,
    private readonly authService: AuthService,
  ) {}

  isBuyer(): boolean {
    return this.authService.isLoggedIn() && this.authService.isClient();
  }

  ngOnInit(): void {
    const productId = this.route.snapshot.paramMap.get('id');
    if (productId) {
      this.loadProduct(productId);
    } else {
      this.error = 'Product not found';
      this.loading = false;
    }
  }

  loadProduct(productId: string): void {
    this.productService.getProductById(productId).subscribe({
      next: (product) => {
        this.product = product;
        this.loading = false;
        this.loadProductMedia(productId);
      },
      error: () => {
        this.error = 'Failed to load product';
        this.loading = false;
      },
    });
  }

  loadProductMedia(productId: string): void {
    this.mediaService.getMediaByProduct(productId).subscribe({
      next: (media) => {
        this.productMedia = media;
      },
      error: () => {
        // Silently fail - product can exist without images
      },
    });
  }

  getProductImageUrls(): string[] {
    return this.productMedia.map((m) => this.mediaService.getMediaFile(m.id!));
  }

  addToCart(): void {
    if (!this.product) return;

    // Get product image for cart display
    const imageUrl =
      this.productMedia.length > 0
        ? this.mediaService.getMediaFile(this.productMedia[0].id!)
        : undefined;

    const cartItem: CartItem = {
      productId: this.product.id!,
      productName: this.product.name,
      sellerId: this.product.user || '', // seller email/id
      price: this.product.price,
      quantity: 1,
      stock: this.product.stock,
      image: imageUrl,
    };

    this.cartService.addToCart(cartItem).subscribe({
      next: () => {
        alert(`"${this.product!.name}" added to cart! 🛒`);
      },
      error: (err) => {
        console.error('Failed to add to cart:', err);
        alert(`Failed to add "${this.product!.name}" to cart. Please try again.`);
      },
    });
  }
}
