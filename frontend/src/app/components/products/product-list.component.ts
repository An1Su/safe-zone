import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { CartItem } from '../../models/cart.model';
import { Media, Product, ProductCategory } from '../../models/ecommerce.model';
import { AuthService } from '../../services/auth.service';
import { CartService } from '../../services/cart.service';
import { MediaService } from '../../services/media.service';
import { ProductSearchParams, ProductSearchResult, ProductService } from '../../services/product.service';
import { ImageSliderComponent } from '../shared/image-slider/image-slider.component';

@Component({
  selector: 'app-product-list',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule, ImageSliderComponent],
  templateUrl: './product-list.component.html',
  styleUrl: './product-list.component.scss',
})
export class ProductListComponent implements OnInit {
  products: Product[] = [];
  productMedia: Map<string, Media[]> = new Map();
  loading = true;
  error = '';

  // Search & Filter state
  searchQuery = '';
  selectedCategory: ProductCategory | null = null;
  minPrice = 0;
  maxPrice = 100;
  priceRange = 100;
  sortBy: 'price_asc' | 'price_desc' | 'name' | 'newest' = 'newest';
  
  // Pagination
  currentPage = 1;
  totalPages = 1;
  totalProducts = 0;
  productsPerPage = 6;

  // View mode
  viewMode: 'grid' | 'list' = 'grid';

  // Categories
  categories: ProductCategory[] = ['Face', 'Eyes', 'Lips'];

  constructor(
    private readonly productService: ProductService,
    private readonly cartService: CartService,
    private readonly mediaService: MediaService,
    private readonly authService: AuthService,
  ) {}

  ngOnInit(): void {
    this.loadProducts();
  }

  loadProducts(): void {
    this.loading = true;
    
    const params: ProductSearchParams = {
      query: this.searchQuery || undefined,
      category: this.selectedCategory || undefined,
      minPrice: this.minPrice > 0 ? this.minPrice : undefined,
      maxPrice: this.priceRange < 100 ? this.priceRange : undefined,
      sortBy: this.sortBy,
      page: this.currentPage,
      limit: this.productsPerPage,
    };

    this.productService.searchProducts(params).subscribe({
      next: (result: ProductSearchResult) => {
        this.products = result.products;
        this.totalProducts = result.total;
        this.totalPages = result.totalPages;
        this.currentPage = result.page;
        this.loading = false;
        
        // Load media for each product
        result.products.forEach((product) => {
          if (product.id) {
            this.loadProductMedia(product.id);
          }
        });
      },
      error: (err) => {
        console.error('Error loading products:', err);
        this.error = 'Failed to load products. Please try again.';
        this.loading = false;
      },
    });
  }

  loadProductMedia(productId: string): void {
    this.mediaService.getMediaByProduct(productId).subscribe({
      next: (media) => {
        this.productMedia.set(productId, media);
      },
      error: (err) => {
        console.error('Error loading media for product:', productId, err);
      },
    });
  }

  getProductImageUrls(productId: string): string[] {
    const media = this.productMedia.get(productId);
    if (media && media.length > 0) {
      return media.map((m) => this.mediaService.getMediaFile(m.id!));
    }
    return [];
  }

  getFirstProductImage(productId: string): string | null {
    const urls = this.getProductImageUrls(productId);
    return urls.length > 0 ? urls[0] : null;
  }

  // Filter methods
  onSearch(): void {
    this.currentPage = 1;
    this.loadProducts();
  }

  onCategoryChange(category: ProductCategory | null): void {
    this.selectedCategory = this.selectedCategory === category ? null : category;
    this.currentPage = 1;
    this.loadProducts();
  }

  onPriceRangeChange(): void {
    this.currentPage = 1;
    this.loadProducts();
  }

  onSortChange(): void {
    this.currentPage = 1;
    this.loadProducts();
  }

  clearFilters(): void {
    this.searchQuery = '';
    this.selectedCategory = null;
    this.minPrice = 0;
    this.priceRange = 100;
    this.sortBy = 'newest';
    this.currentPage = 1;
    this.loadProducts();
  }

  // Pagination
  goToPage(page: number): void {
    if (page >= 1 && page <= this.totalPages) {
      this.currentPage = page;
      this.loadProducts();
    }
  }

  getPageNumbers(): number[] {
    const pages: number[] = [];
    const maxVisible = 5;
    let start = Math.max(1, this.currentPage - Math.floor(maxVisible / 2));
    const end = Math.min(this.totalPages, start + maxVisible - 1);
    
    if (end - start + 1 < maxVisible) {
      start = Math.max(1, end - maxVisible + 1);
    }
    
    for (let i = start; i <= end; i++) {
      pages.push(i);
    }
    return pages;
  }

  // View toggle
  setViewMode(mode: 'grid' | 'list'): void {
    this.viewMode = mode;
  }

  // Cart
  addToCart(product: Product, event: Event): void {
    event.stopPropagation();
    event.preventDefault();

    if (!this.authService.isLoggedIn()) {
      alert('Please log in to add items to cart');
      return;
    }

    const media = this.productMedia.get(product.id!);
    const imageUrl =
      media && media.length > 0 ? this.mediaService.getMediaFile(media[0].id!) : undefined;

    const cartItem: CartItem = {
      productId: product.id!,
      productName: product.name,
      sellerId: product.user || '',
      price: product.price,
      quantity: 1,
      stock: product.stock,
      image: imageUrl,
    };

    this.cartService.addToCart(cartItem).subscribe({
      next: () => {
        alert(`"${product.name}" added to cart! 🛒`);
      },
      error: (err) => {
        console.error('Failed to add to cart:', err);
        alert(`Failed to add "${product.name}" to cart. Please try again.`);
      },
    });
  }

  isBuyer(): boolean {
    return this.authService.isLoggedIn() && this.authService.isClient();
  }
}
