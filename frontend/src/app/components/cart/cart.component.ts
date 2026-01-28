import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { Subscription, forkJoin } from 'rxjs';
import { CartItem, CartItemValidationError } from '../../models/cart.model';
import { Product } from '../../models/ecommerce.model';
import { AuthService } from '../../services/auth.service';
import { CartService } from '../../services/cart.service';
import { MediaService } from '../../services/media.service';
import { ProductService } from '../../services/product.service';

@Component({
  selector: 'app-cart',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './cart.component.html',
  styleUrl: './cart.component.scss',
})
export class CartComponent implements OnInit, OnDestroy {
  cartItems: CartItem[] = [];
  selectedItems: Set<string> = new Set();
  validationErrors: Map<string, CartItemValidationError> = new Map();
  productImages: Map<string, string> = new Map();
  isValidating = false;
  hasValidationIssues = false;

  private cartSubscription?: Subscription;

  constructor(
    private cartService: CartService,
    private productService: ProductService,
    private mediaService: MediaService,
    private authService: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.cartSubscription = this.cartService.cart$.subscribe((cart) => {
      this.cartItems = cart.items;
      this.loadProductImages();
    });

    // Validate stock on load
    this.validateCart();
  }

  ngOnDestroy(): void {
    this.cartSubscription?.unsubscribe();
  }

  get cartTotal(): number {
    return this.cartItems.reduce((sum, item) => sum + item.price * item.quantity, 0);
  }

  get isLoggedIn(): boolean {
    return this.authService.isLoggedIn();
  }

  get canProceed(): boolean {
    return this.cartItems.length > 0 && !this.hasValidationIssues && this.isLoggedIn;
  }

  loadProductImages(): void {
    this.cartItems.forEach((item) => {
      // Use stored image if available, otherwise fetch from media service
      if (item.image) {
        this.productImages.set(item.productId, item.image);
      } else if (!this.productImages.has(item.productId)) {
        this.mediaService.getMediaByProduct(item.productId).subscribe({
          next: (media) => {
            if (media && media.length > 0) {
              this.productImages.set(item.productId, this.mediaService.getMediaFile(media[0].id!));
            }
          },
        });
      }
    });
  }

  getProductImage(productId: string): string | undefined {
    // First check if cart item has image stored
    const item = this.cartItems.find((i) => i.productId === productId);
    if (item?.image) {
      return item.image;
    }
    return this.productImages.get(productId);
  }

  validateCart(): void {
    if (this.cartItems.length === 0) {
      this.hasValidationIssues = false;
      return;
    }

    this.isValidating = true;
    this.validationErrors.clear();

    const productRequests = this.cartItems.map((item) =>
      this.productService.getProductById(item.productId)
    );

    forkJoin(productRequests).subscribe({
      next: (products: Product[]) => {
        this.cartItems.forEach((item, index) => {
          const product = products[index];
          if (!product) {
            this.validationErrors.set(item.productId, {
              productId: item.productId,
              productName: item.productName,
              issue: 'not_found',
              currentStock: 0,
              requestedQuantity: item.quantity,
            });
          } else if (product.stock === 0) {
            this.validationErrors.set(item.productId, {
              productId: item.productId,
              productName: item.productName,
              issue: 'out_of_stock',
              currentStock: 0,
              requestedQuantity: item.quantity,
            });
          } else if (product.stock < item.quantity) {
            this.validationErrors.set(item.productId, {
              productId: item.productId,
              productName: item.productName,
              issue: 'insufficient_stock',
              currentStock: product.stock,
              requestedQuantity: item.quantity,
            });
          }
        });

        this.hasValidationIssues = this.validationErrors.size > 0;
        this.isValidating = false;
      },
      error: () => {
        this.isValidating = false;
      },
    });
  }

  getValidationError(productId: string): CartItemValidationError | undefined {
    return this.validationErrors.get(productId);
  }

  updateQuantity(productId: string, delta: number): void {
    const item = this.cartItems.find((i) => i.productId === productId);
    if (item) {
      const newQuantity = item.quantity + delta;
      if (newQuantity > 0) {
        this.cartService.updateQuantity(productId, newQuantity);
        // Re-validate after quantity change
        this.validateCart();
      }
    }
  }

  setQuantity(productId: string, event: Event): void {
    const input = event.target as HTMLInputElement;
    const quantity = parseInt(input.value, 10);
    if (!isNaN(quantity) && quantity > 0) {
      this.cartService.updateQuantity(productId, quantity);
      this.validateCart();
    }
  }

  toggleItemSelection(productId: string): void {
    if (this.selectedItems.has(productId)) {
      this.selectedItems.delete(productId);
    } else {
      this.selectedItems.add(productId);
    }
  }

  isItemSelected(productId: string): boolean {
    return this.selectedItems.has(productId);
  }

  selectAll(): void {
    if (this.selectedItems.size === this.cartItems.length) {
      this.selectedItems.clear();
    } else {
      this.cartItems.forEach((item) => this.selectedItems.add(item.productId));
    }
  }

  get allSelected(): boolean {
    return this.cartItems.length > 0 && this.selectedItems.size === this.cartItems.length;
  }

  removeSelected(): void {
    if (this.selectedItems.size === 0) return;

    this.selectedItems.forEach((productId) => {
      this.cartService.removeFromCart(productId);
      this.validationErrors.delete(productId);
    });

    this.selectedItems.clear();
    this.hasValidationIssues = this.validationErrors.size > 0;
  }

  removeItem(productId: string): void {
    this.cartService.removeFromCart(productId);
    this.selectedItems.delete(productId);
    this.validationErrors.delete(productId);
    this.hasValidationIssues = this.validationErrors.size > 0;
  }

  adjustToAvailableStock(productId: string): void {
    const error = this.validationErrors.get(productId);
    if (error && error.issue === 'insufficient_stock') {
      this.cartService.updateQuantity(productId, error.currentStock);
      this.validationErrors.delete(productId);
      this.hasValidationIssues = this.validationErrors.size > 0;
    }
  }

  clearCart(): void {
    this.cartService.clearCart();
    this.selectedItems.clear();
    this.validationErrors.clear();
    this.hasValidationIssues = false;
  }

  proceedToCheckout(): void {
    if (!this.isLoggedIn) {
      this.router.navigate(['/login'], { queryParams: { returnUrl: '/cart' } });
      return;
    }

    if (this.canProceed) {
      this.router.navigate(['/checkout']);
    }
  }

  continueShopping(): void {
    this.router.navigate(['/products']);
  }
}

