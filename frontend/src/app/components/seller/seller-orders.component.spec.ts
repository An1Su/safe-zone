import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { RouterTestingModule } from '@angular/router/testing';
import { of, throwError } from 'rxjs';
import { Order, OrderStatus } from '../../models/order.model';
import { AuthService } from '../../services/auth.service';
import { MediaService } from '../../services/media.service';
import { OrderService } from '../../services/order.service';
import { SellerOrdersComponent } from './seller-orders.component';

describe('SellerOrdersComponent', () => {
  let component: SellerOrdersComponent;
  let fixture: ComponentFixture<SellerOrdersComponent>;
  let orderServiceSpy: jasmine.SpyObj<OrderService>;
  let authServiceSpy: jasmine.SpyObj<AuthService>;
  let mediaServiceSpy: jasmine.SpyObj<MediaService>;

  const mockOrders: Order[] = [
    {
      id: 'order-1',
      userId: 'user-1',
      items: [
        {
          productId: 'prod-1',
          productName: 'Lipstick',
          sellerId: 'seller-1',
          price: 25.0,
          quantity: 2,
        },
      ],
      status: 'PENDING',
      totalAmount: 50.0,
      shippingAddress: {
        fullName: 'Jane Doe',
        address: '123 Main St',
        city: 'New York',
        phone: '555-1234',
      },
      createdAt: new Date('2024-01-15'),
    },
    {
      id: 'order-2',
      userId: 'user-2',
      items: [
        {
          productId: 'prod-2',
          productName: 'Foundation',
          sellerId: 'seller-1',
          price: 45.0,
          quantity: 1,
        },
      ],
      status: 'SHIPPED',
      totalAmount: 45.0,
      shippingAddress: {
        fullName: 'John Smith',
        address: '456 Oak Ave',
        city: 'Los Angeles',
        phone: '555-5678',
      },
      createdAt: new Date('2024-01-10'),
    },
    {
      id: 'order-3',
      userId: 'user-3',
      items: [
        {
          productId: 'prod-3',
          productName: 'Mascara',
          sellerId: 'seller-1',
          price: 20.0,
          quantity: 3,
        },
      ],
      status: 'DELIVERED',
      totalAmount: 60.0,
      shippingAddress: {
        fullName: 'Alice Johnson',
        address: '789 Pine Rd',
        city: 'Chicago',
        phone: '555-9012',
      },
      createdAt: new Date('2024-01-05'),
    },
    {
      id: 'order-4',
      userId: 'user-4',
      items: [
        {
          productId: 'prod-4',
          productName: 'Eyeshadow',
          sellerId: 'seller-1',
          price: 30.0,
          quantity: 1,
        },
      ],
      status: 'CANCELLED',
      totalAmount: 30.0,
      shippingAddress: {
        fullName: 'Bob Wilson',
        address: '321 Elm St',
        city: 'Houston',
        phone: '555-3456',
      },
      createdAt: new Date('2024-01-01'),
    },
  ];

  beforeEach(async () => {
    orderServiceSpy = jasmine.createSpyObj('OrderService', [
      'getSellerOrders',
      'updateOrderStatus',
    ]);
    authServiceSpy = jasmine.createSpyObj('AuthService', ['getAuthHeaders']);
    mediaServiceSpy = jasmine.createSpyObj('MediaService', [
      'getMediaByProduct',
      'getMediaFile',
    ]);

    orderServiceSpy.getSellerOrders.and.returnValue(of(mockOrders));
    mediaServiceSpy.getMediaByProduct.and.returnValue(of([]));
    mediaServiceSpy.getMediaFile.and.returnValue('http://example.com/image.jpg');

    await TestBed.configureTestingModule({
      imports: [SellerOrdersComponent, RouterTestingModule, FormsModule],
      providers: [
        { provide: OrderService, useValue: orderServiceSpy },
        { provide: AuthService, useValue: authServiceSpy },
        { provide: MediaService, useValue: mediaServiceSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(SellerOrdersComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('ngOnInit', () => {
    it('should load orders on init', () => {
      fixture.detectChanges();
      expect(orderServiceSpy.getSellerOrders).toHaveBeenCalled();
      expect(component.orders.length).toBe(4);
      expect(component.loading).toBeFalse();
    });

    it('should handle error when loading orders', () => {
      orderServiceSpy.getSellerOrders.and.returnValue(
        throwError(() => new Error('Network error'))
      );
      fixture.detectChanges();
      expect(component.error).toBe('Failed to load orders. Please try again.');
      expect(component.loading).toBeFalse();
    });
  });

  describe('calculateStats', () => {
    it('should calculate stats correctly', () => {
      fixture.detectChanges();
      expect(component.stats.totalOrders).toBe(4);
      expect(component.stats.pending).toBe(1); // PENDING + READY_FOR_DELIVERY
      expect(component.stats.shipped).toBe(1);
      expect(component.stats.delivered).toBe(1);
      // totalRevenue excludes CANCELLED orders: 50 + 45 + 60 = 155
      expect(component.stats.totalRevenue).toBe(155);
    });

    it('should handle empty orders', () => {
      orderServiceSpy.getSellerOrders.and.returnValue(of([]));
      fixture.detectChanges();
      expect(component.stats.totalOrders).toBe(0);
      expect(component.stats.totalRevenue).toBe(0);
    });
  });

  describe('applyFilters', () => {
    beforeEach(() => {
      fixture.detectChanges();
    });

    it('should filter by status', () => {
      component.statusFilter = 'PENDING';
      component.applyFilters();
      expect(component.filteredOrders.length).toBe(1);
      expect(component.filteredOrders[0].status).toBe('PENDING');
    });

    it('should filter by search query (order ID)', () => {
      component.searchQuery = 'order-1';
      component.applyFilters();
      expect(component.filteredOrders.length).toBe(1);
      expect(component.filteredOrders[0].id).toBe('order-1');
    });

    it('should filter by search query (product name)', () => {
      component.searchQuery = 'lipstick';
      component.applyFilters();
      expect(component.filteredOrders.length).toBe(1);
      expect(component.filteredOrders[0].items[0].productName).toBe('Lipstick');
    });

    it('should filter by search query (customer name)', () => {
      component.searchQuery = 'jane';
      component.applyFilters();
      expect(component.filteredOrders.length).toBe(1);
      expect(component.filteredOrders[0].shippingAddress.fullName).toBe('Jane Doe');
    });

    it('should combine status and search filters', () => {
      component.statusFilter = 'DELIVERED';
      component.searchQuery = 'mascara';
      component.applyFilters();
      expect(component.filteredOrders.length).toBe(1);
    });

    it('should return empty array when no match', () => {
      component.searchQuery = 'nonexistent';
      component.applyFilters();
      expect(component.filteredOrders.length).toBe(0);
    });
  });

  describe('clearFilters', () => {
    it('should clear all filters', () => {
      fixture.detectChanges();
      component.statusFilter = 'PENDING';
      component.searchQuery = 'test';
      component.applyFilters();

      component.clearFilters();

      expect(component.statusFilter).toBe('');
      expect(component.searchQuery).toBe('');
      expect(component.filteredOrders.length).toBe(4);
    });
  });

  describe('updateOrderStatus', () => {
    beforeEach(() => {
      fixture.detectChanges();
    });

    it('should update order status successfully', () => {
      const updatedOrder = { ...mockOrders[0], status: 'READY_FOR_DELIVERY' as OrderStatus };
      orderServiceSpy.updateOrderStatus.and.returnValue(of(updatedOrder));

      component.updateOrderStatus(mockOrders[0], 'READY_FOR_DELIVERY');

      expect(orderServiceSpy.updateOrderStatus).toHaveBeenCalledWith(
        'order-1',
        'READY_FOR_DELIVERY'
      );
      expect(component.successMessage).toContain('Ready for Delivery');
    });

    it('should handle error when updating status', () => {
      orderServiceSpy.updateOrderStatus.and.returnValue(
        throwError(() => new Error('Update failed'))
      );

      component.updateOrderStatus(mockOrders[0], 'READY_FOR_DELIVERY');

      expect(component.error).toBe('Failed to update order status. Please try again.');
    });

    it('should not update if order has no ID', () => {
      const orderWithoutId = { ...mockOrders[0], id: undefined };
      component.updateOrderStatus(orderWithoutId, 'READY_FOR_DELIVERY');
      expect(orderServiceSpy.updateOrderStatus).not.toHaveBeenCalled();
    });
  });

  describe('canUpdateStatus', () => {
    it('should return true for PENDING orders', () => {
      expect(component.canUpdateStatus({ status: 'PENDING' } as Order)).toBeTrue();
    });

    it('should return true for READY_FOR_DELIVERY orders', () => {
      expect(component.canUpdateStatus({ status: 'READY_FOR_DELIVERY' } as Order)).toBeTrue();
    });

    it('should return true for SHIPPED orders', () => {
      expect(component.canUpdateStatus({ status: 'SHIPPED' } as Order)).toBeTrue();
    });

    it('should return false for DELIVERED orders', () => {
      expect(component.canUpdateStatus({ status: 'DELIVERED' } as Order)).toBeFalse();
    });

    it('should return false for CANCELLED orders', () => {
      expect(component.canUpdateStatus({ status: 'CANCELLED' } as Order)).toBeFalse();
    });
  });

  describe('getNextStatuses', () => {
    it('should return correct next statuses for PENDING', () => {
      const nextStatuses = component.getNextStatuses({ status: 'PENDING' } as Order);
      expect(nextStatuses).toEqual(['READY_FOR_DELIVERY', 'CANCELLED']);
    });

    it('should return correct next statuses for READY_FOR_DELIVERY', () => {
      const nextStatuses = component.getNextStatuses({ status: 'READY_FOR_DELIVERY' } as Order);
      expect(nextStatuses).toEqual(['SHIPPED', 'CANCELLED']);
    });

    it('should return correct next statuses for SHIPPED', () => {
      const nextStatuses = component.getNextStatuses({ status: 'SHIPPED' } as Order);
      expect(nextStatuses).toEqual(['DELIVERED']);
    });

    it('should return empty array for DELIVERED', () => {
      const nextStatuses = component.getNextStatuses({ status: 'DELIVERED' } as Order);
      expect(nextStatuses).toEqual([]);
    });
  });

  describe('getStatusClass', () => {
    it('should return correct class for PENDING', () => {
      expect(component.getStatusClass('PENDING')).toBe('status-pending');
    });

    it('should return correct class for READY_FOR_DELIVERY', () => {
      expect(component.getStatusClass('READY_FOR_DELIVERY')).toBe('status-ready');
    });

    it('should return correct class for SHIPPED', () => {
      expect(component.getStatusClass('SHIPPED')).toBe('status-shipped');
    });

    it('should return correct class for DELIVERED', () => {
      expect(component.getStatusClass('DELIVERED')).toBe('status-delivered');
    });

    it('should return correct class for CANCELLED', () => {
      expect(component.getStatusClass('CANCELLED')).toBe('status-cancelled');
    });
  });

  describe('getStatusLabel', () => {
    it('should return correct label for PENDING', () => {
      expect(component.getStatusLabel('PENDING')).toBe('Pending');
    });

    it('should return correct label for READY_FOR_DELIVERY', () => {
      expect(component.getStatusLabel('READY_FOR_DELIVERY')).toBe('Ready for Delivery');
    });

    it('should return correct label for SHIPPED', () => {
      expect(component.getStatusLabel('SHIPPED')).toBe('Shipped');
    });

    it('should return correct label for DELIVERED', () => {
      expect(component.getStatusLabel('DELIVERED')).toBe('Delivered');
    });

    it('should return correct label for CANCELLED', () => {
      expect(component.getStatusLabel('CANCELLED')).toBe('Cancelled');
    });
  });

  describe('formatDate', () => {
    it('should format Date object', () => {
      const date = new Date('2024-01-15');
      const formatted = component.formatDate(date);
      expect(formatted).toContain('January');
      expect(formatted).toContain('15');
      expect(formatted).toContain('2024');
    });

    it('should format date string', () => {
      const formatted = component.formatDate('2024-01-15');
      expect(formatted).toContain('January');
      expect(formatted).toContain('15');
      expect(formatted).toContain('2024');
    });

    it('should return N/A for undefined date', () => {
      expect(component.formatDate(undefined)).toBe('N/A');
    });
  });

  describe('getOrderNumber', () => {
    it('should generate order number from date and id', () => {
      const order = {
        id: 'abc123xyz',
        createdAt: new Date('2024-01-15'),
      } as Order;
      const orderNumber = component.getOrderNumber(order);
      expect(orderNumber).toBe('ORD-2024-xyz');
    });

    it('should use id suffix if no createdAt', () => {
      const order = {
        id: 'abc123xyz',
        createdAt: undefined,
      } as Order;
      const orderNumber = component.getOrderNumber(order);
      expect(orderNumber).toBe('bc123xyz'); // slice(-8) of 'abc123xyz'
    });

    it('should return N/A if no id and no createdAt', () => {
      const order = {
        id: undefined,
        createdAt: undefined,
      } as Order;
      const orderNumber = component.getOrderNumber(order);
      expect(orderNumber).toBe('N/A');
    });
  });

  describe('loadProductImages', () => {
    it('should load images for all products', () => {
      mediaServiceSpy.getMediaByProduct.and.returnValue(
        of([
          {
            id: 'media-1',
            imagePath: '/images/test.jpg',
            productId: 'prod-1',
            fileName: 'test.jpg',
            contentType: 'image/jpeg',
            fileSize: 1000,
          },
        ])
      );

      fixture.detectChanges();

      // Should be called for each unique productId
      expect(mediaServiceSpy.getMediaByProduct).toHaveBeenCalled();
    });

    it('should handle media loading errors silently', () => {
      mediaServiceSpy.getMediaByProduct.and.returnValue(
        throwError(() => new Error('Media not found'))
      );

      fixture.detectChanges();

      // Should not throw error
      expect(component.productImages.size).toBe(0);
    });
  });

  describe('getProductImage', () => {
    it('should return image URL if exists', () => {
      component.productImages.set('prod-1', 'http://example.com/image.jpg');
      expect(component.getProductImage('prod-1')).toBe('http://example.com/image.jpg');
    });

    it('should return undefined if no image', () => {
      expect(component.getProductImage('nonexistent')).toBeUndefined();
    });
  });

  describe('onFilterChange', () => {
    it('should call applyFilters', () => {
      fixture.detectChanges();
      spyOn(component, 'applyFilters');
      component.onFilterChange();
      expect(component.applyFilters).toHaveBeenCalled();
    });
  });
});

