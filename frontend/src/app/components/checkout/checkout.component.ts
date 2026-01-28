import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { Subscription } from 'rxjs';
import { CartItem } from '../../models/cart.model';
import { ShippingAddress } from '../../models/order.model';
import { AuthService } from '../../services/auth.service';
import { CartService } from '../../services/cart.service';
import { MediaService } from '../../services/media.service';
import { OrderService } from '../../services/order.service';

@Component({
  selector: 'app-checkout',
  standalone: true,
  imports: [CommonModule, RouterModule, ReactiveFormsModule],
  templateUrl: './checkout.component.html',
  styleUrl: './checkout.component.scss',
})
export class CheckoutComponent implements OnInit, OnDestroy {
  shippingForm: FormGroup;
  cartItems: CartItem[] = [];
  productImages: Map<string, string> = new Map();
  isSubmitting = false;
  errorMessage = '';

  private cartSubscription?: Subscription;

  constructor(
    private readonly fb: FormBuilder,
    private readonly cartService: CartService,
    private readonly orderService: OrderService,
    private readonly authService: AuthService,
    private readonly mediaService: MediaService,
    private readonly router: Router,
  ) {
    this.shippingForm = this.fb.group({
      fullName: ['', [Validators.required, Validators.minLength(2)]],
      address: ['', [Validators.required, Validators.minLength(5)]],
      city: ['', [Validators.required, Validators.minLength(2)]],
      phone: ['', [Validators.required, Validators.pattern(/^\+?[\d\s-]{8,}$/)]],
    });
  }

  ngOnInit(): void {
    // Check if user is logged in
    if (!this.authService.isLoggedIn()) {
      this.router.navigate(['/login'], { queryParams: { returnUrl: '/checkout' } });
      return;
    }

    // Load cart from backend
    this.cartService.loadCart().subscribe({
      error: (err) => {
        console.error('Failed to load cart:', err);
      },
    });

    this.cartSubscription = this.cartService.cart$.subscribe((cart) => {
      this.cartItems = cart.items;

      // Redirect to cart if empty
      if (this.cartItems.length === 0) {
        this.router.navigate(['/cart']);
      }

      this.loadProductImages();
    });

    // Pre-fill name from user profile
    const user = this.authService.getCurrentUser();
    if (user?.name) {
      this.shippingForm.patchValue({ fullName: user.name });
    }
  }

  ngOnDestroy(): void {
    this.cartSubscription?.unsubscribe();
  }

  get cartTotal(): number {
    return this.cartItems.reduce((sum, item) => sum + item.price * item.quantity, 0);
  }

  get fullName() {
    return this.shippingForm.get('fullName');
  }
  get address() {
    return this.shippingForm.get('address');
  }
  get city() {
    return this.shippingForm.get('city');
  }
  get phone() {
    return this.shippingForm.get('phone');
  }

  loadProductImages(): void {
    this.cartItems.forEach((item) => {
      if (!this.productImages.has(item.productId)) {
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
    return this.productImages.get(productId);
  }

  onSubmit(): void {
    if (this.shippingForm.invalid) {
      // Mark all fields as touched to show validation errors
      Object.keys(this.shippingForm.controls).forEach((key) => {
        this.shippingForm.get(key)?.markAsTouched();
      });
      return;
    }

    this.isSubmitting = true;
    this.errorMessage = '';

    const shippingAddress: ShippingAddress = {
      fullName: this.shippingForm.value.fullName,
      address: this.shippingForm.value.address,
      city: this.shippingForm.value.city,
      phone: this.shippingForm.value.phone,
    };

    // Backend creates order from cart automatically, so we only send shipping address
    this.orderService.createOrder(shippingAddress).subscribe({
      next: (order) => {
        // Clear the cart after successful order
        this.cartService.clearCart().subscribe({
          next: () => {
            // Navigate to confirmation page with order ID
            this.router.navigate(['/order-confirmation', order.id]);
          },
          error: (err) => {
            console.error('Failed to clear cart:', err);
            // Still navigate even if cart clear fails
            this.router.navigate(['/order-confirmation', order.id]);
          },
        });
      },
      error: (error) => {
        this.isSubmitting = false;
        if (error.status === 400) {
          this.errorMessage =
            error.error?.message ||
            'Some products are no longer available. Please review your cart.';
        } else if (error.status === 401) {
          this.router.navigate(['/login'], { queryParams: { returnUrl: '/checkout' } });
        } else {
          this.errorMessage = 'Failed to place order. Please try again.';
        }
      },
    });
  }

  goBack(): void {
    this.router.navigate(['/cart']);
  }
}
