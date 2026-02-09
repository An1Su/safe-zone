import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { Order, OrderStatus } from '../../models/order.model';
import { AuthService } from '../../services/auth.service';
import { MediaService } from '../../services/media.service';
import { OrderService } from '../../services/order.service';

interface BuyerStats {
  totalOrders: number;
  delivered: number;
  inProgress: number;
  totalSpent: number;
}

interface SellerStats {
  totalOrders: number;
  pending: number;
  shipped: number;
  delivered: number;
  totalRevenue: number;
}

@Component({
  selector: 'app-order-list',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './order-list.component.html',
  styleUrl: './order-list.component.scss',
})
export class OrderListComponent implements OnInit {
  // Role-based mode
  isSeller = false;

  // Orders
  orders: Order[] = [];
  displayOrders: Order[] = [];
  loading = true;
  error = '';
  successMessage = '';

  // Stats
  buyerStats: BuyerStats = {
    totalOrders: 0,
    delivered: 0,
    inProgress: 0,
    totalSpent: 0,
  };
  sellerStats: SellerStats = {
    totalOrders: 0,
    pending: 0,
    shipped: 0,
    delivered: 0,
    totalRevenue: 0,
  };

  // Product images cache
  productImages: Map<string, string> = new Map();

  // Filters (seller only)
  searchQuery = '';
  statusFilter: OrderStatus | '' = '';

  // Status options for dropdown (seller only)
  statusOptions: { value: OrderStatus | ''; label: string }[] = [
    { value: '', label: 'All Statuses' },
    { value: 'PENDING', label: 'Pending' },
    { value: 'READY_FOR_DELIVERY', label: 'Ready for Delivery' },
    { value: 'SHIPPED', label: 'Shipped' },
    { value: 'DELIVERED', label: 'Delivered' },
    { value: 'CANCELLED', label: 'Cancelled' },
  ];

  // Status update options (seller only)
  nextStatusOptions: Record<OrderStatus, OrderStatus[]> = {
    'PENDING': ['READY_FOR_DELIVERY', 'CANCELLED'],
    'READY_FOR_DELIVERY': ['SHIPPED', 'CANCELLED'],
    'SHIPPED': ['DELIVERED'],
    'DELIVERED': [],
    'CANCELLED': [],
  };

  constructor(
    private readonly orderService: OrderService,
    private readonly mediaService: MediaService,
    private readonly authService: AuthService
  ) {}

  ngOnInit(): void {
    this.isSeller = this.authService.isSeller();
    this.loadOrders();
  }

  loadOrders(): void {
    this.loading = true;
    this.error = '';

    const orderObservable = this.isSeller
      ? this.orderService.getSellerOrders()
      : this.orderService.getOrders();

    orderObservable.subscribe({
      next: (orders) => {
        // Sort by date (newest first) for buyers
        if (!this.isSeller) {
          orders = [...orders].sort((a, b) => {
            const dateA = new Date(a.createdAt || 0).getTime();
            const dateB = new Date(b.createdAt || 0).getTime();
            return dateB - dateA;
          });
        }

        this.orders = orders;
        this.applyFilters();
        this.calculateStats();
        this.loadProductImages();
        this.loading = false;
      },
      error: (err) => {
        console.error('Failed to load orders:', err);
        this.error = 'Failed to load orders. Please try again.';
        this.loading = false;
      },
    });
  }

  calculateStats(): void {
    if (this.isSeller) {
      this.sellerStats = {
        totalOrders: this.orders.length,
        pending: this.orders.filter(
          (o) => o.status === 'PENDING' || o.status === 'READY_FOR_DELIVERY'
        ).length,
        shipped: this.orders.filter((o) => o.status === 'SHIPPED').length,
        delivered: this.orders.filter((o) => o.status === 'DELIVERED').length,
        totalRevenue: this.orders
          .filter((o) => o.status !== 'CANCELLED')
          .reduce((sum, o) => sum + o.totalAmount, 0),
      };
    } else {
      this.buyerStats = {
        totalOrders: this.orders.length,
        delivered: this.orders.filter((o) => o.status === 'DELIVERED').length,
        inProgress: this.orders.filter((o) =>
          ['PENDING', 'READY_FOR_DELIVERY', 'SHIPPED'].includes(o.status)
        ).length,
        totalSpent: this.orders
          .filter((o) => o.status !== 'CANCELLED')
          .reduce((sum, o) => sum + o.totalAmount, 0),
      };
    }
  }

  loadProductImages(): void {
    const productIds = new Set<string>();
    this.orders.forEach((order) => {
      order.items.forEach((item) => {
        if (!this.productImages.has(item.productId)) {
          productIds.add(item.productId);
        }
      });
    });

    productIds.forEach((productId) => {
      this.mediaService.getMediaByProduct(productId).subscribe({
        next: (media) => {
          if (media && media.length > 0) {
            this.productImages.set(
              productId,
              this.mediaService.getMediaFile(media[0].id!)
            );
          }
        },
        error: () => {
          // Silently fail for missing images
        },
      });
    });
  }

  // Filter methods (seller only)
  applyFilters(): void {
    if (!this.isSeller) {
      this.displayOrders = this.orders;
      return;
    }

    let result = [...this.orders];
    if (this.statusFilter) {
      result = result.filter((o) => o.status === this.statusFilter);
    }
    if (this.searchQuery.trim()) {
      const query = this.searchQuery.toLowerCase();
      result = result.filter(
        (o) =>
          o.id?.toLowerCase().includes(query) ||
          o.items.some((item) => item.productName.toLowerCase().includes(query)) ||
          o.shippingAddress.fullName.toLowerCase().includes(query)
      );
    }
    this.displayOrders = result;
  }

  clearFilters(): void {
    this.searchQuery = '';
    this.statusFilter = '';
    this.applyFilters(); // Reapply filters to existing data instead of reloading
  }

  // Order actions
  cancelOrder(order: Order): void {
    if (!order.id || !confirm('Are you sure you want to cancel this order?')) {
      return;
    }

    this.orderService.cancelOrder(order.id).subscribe({
      next: (updatedOrder) => {
        const index = this.orders.findIndex((o) => o.id === order.id);
        if (index !== -1) {
          this.orders[index] = updatedOrder;
          this.calculateStats();
          this.applyFilters(); // Update displayOrders to reflect the change
        }
      },
      error: (err) => {
        console.error('Failed to cancel order:', err);
        alert('Failed to cancel order. Please try again.');
      },
    });
  }

  updateOrderStatus(order: Order, newStatus: OrderStatus): void {
    if (!order.id || !this.isSeller) return;

    this.orderService.updateOrderStatus(order.id, newStatus).subscribe({
      next: (updatedOrder) => {
        const index = this.orders.findIndex((o) => o.id === order.id);
        if (index !== -1) {
          this.orders[index] = updatedOrder;
          this.calculateStats();
          this.applyFilters(); // Update displayOrders to reflect the change
          this.successMessage = `Order status updated to ${this.getStatusLabel(newStatus)}`;
          setTimeout(() => (this.successMessage = ''), 3000);
        } else {
          // Order not found in local state - reload orders to sync
          console.warn('Order not found in local state, reloading orders...');
          this.loadOrders();
        }
      },
      error: (err) => {
        console.error('Failed to update order status:', err);
        this.error = 'Failed to update order status. Please try again.';
        setTimeout(() => (this.error = ''), 3000);
      },
    });
  }


  // Utility methods
  getStatusClass(status: OrderStatus): string {
    const statusClasses: Record<OrderStatus, string> = {
      PENDING: 'status-pending',
      READY_FOR_DELIVERY: 'status-ready',
      SHIPPED: 'status-shipped',
      DELIVERED: 'status-delivered',
      CANCELLED: 'status-cancelled',
    };
    return statusClasses[status] || 'status-pending';
  }

  getStatusLabel(status: OrderStatus): string {
    const labels: Record<OrderStatus, string> = {
      PENDING: this.isSeller ? 'Pending' : 'Processing',
      READY_FOR_DELIVERY: this.isSeller ? 'Ready for Delivery' : 'Ready',
      SHIPPED: 'Shipped',
      DELIVERED: 'Delivered',
      CANCELLED: 'Cancelled',
    };
    return labels[status] || status;
  }

  formatDate(date: Date | string | undefined): string {
    if (!date) {
      // Sellers expect "N/A" for missing dates, buyers use empty string
      return this.isSeller ? 'N/A' : '';
    }
    const d = typeof date === 'string' ? new Date(date) : date;
    return d.toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'long',
      day: 'numeric',
    });
  }

  getOrderNumber(order: Order): string {
    if (!order.id) {
      // Sellers expect "N/A" for missing order IDs, buyers use empty string
      return this.isSeller ? 'N/A' : '';
    }
    
    // For sellers: when createdAt is missing, return last 8 characters (original behavior)
    // For buyers: always use formatted order number
    if (this.isSeller && !order.createdAt) {
      return order.id.slice(-8);
    }
    
    // Use order creation date year if available, otherwise fallback to current year
    const year = order.createdAt
      ? new Date(order.createdAt).getFullYear()
      : new Date().getFullYear();
    return `ORD-${year}-${order.id.slice(-3).toUpperCase()}`;
  }

  hasStatusOptions(order: Order): boolean {
    const options = this.nextStatusOptions[order.status];
    return options !== undefined && options.length > 0;
  }

  isOrderCompleted(order: Order): boolean {
    const options = this.nextStatusOptions[order.status];
    return options === undefined || options.length === 0;
  }
}
