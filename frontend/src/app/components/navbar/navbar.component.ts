import { CommonModule } from '@angular/common';
import { Component, ElementRef, HostListener, OnDestroy, OnInit } from '@angular/core';
import { Router, RouterModule } from '@angular/router';
import { Subscription } from 'rxjs';
import { User } from '../../models/ecommerce.model';
import { AuthService } from '../../services/auth.service';
import { CartService } from '../../services/cart.service';
import { MediaService } from '../../services/media.service';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './navbar.component.html',
  styleUrls: ['./navbar.component.scss'],
})
export class NavbarComponent implements OnInit, OnDestroy {
  currentUser: User | null = null;
  dropdownOpen = false;
  avatarUrl: string | null = null;
  cartItemCount = 0;

  private cartSubscription?: Subscription;

  constructor(
    private readonly authService: AuthService,
    private readonly mediaService: MediaService,
    private readonly cartService: CartService,
    private readonly router: Router,
    private readonly elementRef: ElementRef
  ) {}

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    // Close dropdown if click is outside the navbar component
    if (this.dropdownOpen && !this.elementRef.nativeElement.contains(event.target)) {
      this.dropdownOpen = false;
    }
  }

  ngOnInit(): void {
    this.authService.currentUser$.subscribe((user) => {
      this.currentUser = user;
      this.loadAvatar();
    });

    // Subscribe to cart changes for badge count
    this.cartSubscription = this.cartService.cart$.subscribe((cart) => {
      this.cartItemCount = cart.items.reduce((sum, item) => sum + item.quantity, 0);
    });
  }

  ngOnDestroy(): void {
    this.cartSubscription?.unsubscribe();
  }

  private loadAvatar(): void {
    if (this.currentUser?.avatar && this.currentUser?.role === 'seller') {
      this.avatarUrl = this.mediaService.getAvatarFileUrl(this.currentUser.avatar);
    } else {
      this.avatarUrl = null;
    }
  }

  getInitials(): string {
    if (!this.currentUser?.name) return 'U';
    return this.currentUser.name
      .split(' ')
      .map((n) => n[0])
      .join('')
      .toUpperCase()
      .substring(0, 2);
  }

  isLoggedIn(): boolean {
    return this.authService.isLoggedIn();
  }

  isSeller(): boolean {
    return this.authService.isSeller();
  }

  isBuyer(): boolean {
    return this.isLoggedIn() && !this.isSeller();
  }

  toggleDropdown(): void {
    this.dropdownOpen = !this.dropdownOpen;
  }

  closeDropdown(): void {
    this.dropdownOpen = false;
  }

  logout(): void {
    this.closeDropdown();
    // Auth service handles logout + hard refresh
    // Subscribe to trigger the request (hard refresh happens in service)
    this.authService.logout().subscribe();
  }
}
