import { ComponentFixture, TestBed } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { of, throwError } from 'rxjs';
import { Order, OrderStatus } from '../../models/order.model';
import { MediaService } from '../../services/media.service';
import { OrderService } from '../../services/order.service';
import { OrderHistoryComponent } from './order-history.component';

describe('OrderHistoryComponent', () => {
  let component: OrderHistoryComponent;
  let fixture: ComponentFixture<OrderHistoryComponent>;
  let orderServiceSpy: jasmine.SpyObj<OrderService>;
  let mediaServiceSpy: jasmine.SpyObj<MediaService>;

  const createMockOrders = (): Order[] => [
    {
      id: 'order-001',
      userId: 'user-1',
      items: [
        {
          productId: 'prod-1',
          productName: 'Velvet Matte Lipstick',
          sellerId: 'seller-1',
          price: 24.99,
          quantity: 2,
        },
        {
          productId: 'prod-2',
          productName: 'Rose Blush Palette',
          sellerId: 'seller-1',
          price: 39.99,
          quantity: 1,
        },
      ],
      status: 'DELIVERED',
      totalAmount: 89.97,
      shippingAddress: {
        fullName: 'Test User',
        address: '123 Test St',
        city: 'Test City',
        phone: '555-0123',
      },
      createdAt: new Date('2024-01-25'),
    },
    {
      id: 'order-002',
      userId: 'user-1',
      items: [
        {
          productId: 'prod-3',
          productName: 'Luxe Mascara',
          sellerId: 'seller-2',
          price: 29.99,
          quantity: 1,
        },
      ],
      status: 'SHIPPED',
      totalAmount: 29.99,
      shippingAddress: {
        fullName: 'Test User',
        address: '123 Test St',
        city: 'Test City',
        phone: '555-0123',
      },
      createdAt: new Date('2024-01-20'),
    },
    {
      id: 'order-003',
      userId: 'user-1',
      items: [
        {
          productId: 'prod-4',
          productName: 'Sunset Eyeshadow Palette',
          sellerId: 'seller-1',
          price: 54.99,
          quantity: 1,
        },
      ],
      status: 'PENDING',
      totalAmount: 54.99,
      shippingAddress: {
        fullName: 'Test User',
        address: '123 Test St',
        city: 'Test City',
        phone: '555-0123',
      },
      createdAt: new Date('2024-01-15'),
    },
  ];

  let mockOrders: Order[];

  beforeEach(async () => {
    const orderSpy = jasmine.createSpyObj('OrderService', ['getOrders', 'cancelOrder']);
    const mediaSpy = jasmine.createSpyObj('MediaService', ['getMediaByProduct', 'getMediaFile']);
    mediaSpy.getMediaByProduct.and.returnValue(of([]));

    await TestBed.configureTestingModule({
      imports: [OrderHistoryComponent, RouterTestingModule],
      providers: [
        { provide: OrderService, useValue: orderSpy },
        { provide: MediaService, useValue: mediaSpy },
      ],
    }).compileComponents();

    orderServiceSpy = TestBed.inject(OrderService) as jasmine.SpyObj<OrderService>;
    mediaServiceSpy = TestBed.inject(MediaService) as jasmine.SpyObj<MediaService>;
  });

  beforeEach(() => {
    // Create fresh copy of mock orders for each test
    mockOrders = createMockOrders();
    orderServiceSpy.getOrders.and.returnValue(of(mockOrders));
    fixture = TestBed.createComponent(OrderHistoryComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    fixture.detectChanges();
    expect(component).toBeTruthy();
  });

  describe('Loading orders', () => {
    it('should load orders on init', () => {
      fixture.detectChanges();

      expect(orderServiceSpy.getOrders).toHaveBeenCalled();
      expect(component.orders.length).toBe(3);
      expect(component.loading).toBeFalse();
    });

    it('should sort orders by date descending', () => {
      fixture.detectChanges();

      expect(component.orders[0].id).toBe('order-001');
      expect(component.orders[1].id).toBe('order-002');
      expect(component.orders[2].id).toBe('order-003');
    });

    it('should handle error when loading orders', () => {
      orderServiceSpy.getOrders.and.returnValue(throwError(() => new Error('Network error')));
      fixture.detectChanges();

      expect(component.error).toBe('Failed to load your order history. Please try again.');
      expect(component.loading).toBeFalse();
    });
  });

  describe('Statistics calculation', () => {
    it('should calculate stats correctly', () => {
      fixture.detectChanges();

      expect(component.stats.totalOrders).toBe(3);
      expect(component.stats.delivered).toBe(1);
      expect(component.stats.inProgress).toBe(2);
      expect(component.stats.totalSpent).toBe(174.95);
    });

    it('should not count cancelled orders in total spent', () => {
      const ordersWithCancelled: Order[] = [
        ...createMockOrders(),
        {
          id: 'order-004',
          userId: 'user-1',
          items: [],
          status: 'CANCELLED',
          totalAmount: 100,
          shippingAddress: {
            fullName: 'Test',
            address: 'Test',
            city: 'Test',
            phone: '123',
          },
          createdAt: new Date(),
        },
      ];
      orderServiceSpy.getOrders.and.returnValue(of(ordersWithCancelled));
      fixture.detectChanges();

      expect(component.stats.totalOrders).toBe(4);
      expect(component.stats.totalSpent).toBe(174.95);
    });
  });

  describe('Status handling', () => {
    it('should return correct status class', () => {
      expect(component.getStatusClass('PENDING')).toBe('status-pending');
      expect(component.getStatusClass('DELIVERED')).toBe('status-delivered');
      expect(component.getStatusClass('SHIPPED')).toBe('status-shipped');
      expect(component.getStatusClass('CANCELLED')).toBe('status-cancelled');
      expect(component.getStatusClass('READY_FOR_DELIVERY')).toBe('status-ready');
    });

    it('should return correct status labels', () => {
      expect(component.getStatusLabel('PENDING')).toBe('Processing');
      expect(component.getStatusLabel('DELIVERED')).toBe('Delivered');
      expect(component.getStatusLabel('SHIPPED')).toBe('Shipped');
      expect(component.getStatusLabel('CANCELLED')).toBe('Cancelled');
      expect(component.getStatusLabel('READY_FOR_DELIVERY')).toBe('Ready');
    });
  });

  describe('Date formatting', () => {
    it('should format date correctly', () => {
      const formatted = component.formatDate(new Date('2024-01-25'));
      expect(formatted).toContain('January');
      expect(formatted).toContain('25');
      expect(formatted).toContain('2024');
    });

    it('should return empty string for undefined date', () => {
      expect(component.formatDate(undefined)).toBe('');
    });
  });

  describe('Order number generation', () => {
    it('should generate order number with year and ID suffix', () => {
      fixture.detectChanges();

      const orderNumber = component.getOrderNumber(component.orders[0]);
      expect(orderNumber).toContain('ORD-2024-');
      expect(orderNumber).toContain('001');
    });

    it('should return empty string for order without ID', () => {
      const order: Order = {
        ...createMockOrders()[0],
        id: undefined,
      };
      expect(component.getOrderNumber(order)).toBe('');
    });
  });

  describe('Cancel order', () => {
    it('should allow cancelling pending orders', () => {
      fixture.detectChanges();

      const pendingOrder = component.orders.find((o) => o.status === 'PENDING');
      expect(component.canCancel(pendingOrder!)).toBeTrue();
    });

    it('should not allow cancelling delivered orders', () => {
      fixture.detectChanges();

      const deliveredOrder = component.orders.find((o) => o.status === 'DELIVERED');
      expect(component.canCancel(deliveredOrder!)).toBeFalse();
    });

    it('should not allow cancelling shipped orders', () => {
      fixture.detectChanges();

      const shippedOrder = component.orders.find((o) => o.status === 'SHIPPED');
      expect(component.canCancel(shippedOrder!)).toBeFalse();
    });

    it('should cancel order and update list', () => {
      fixture.detectChanges();

      const pendingOrder = component.orders.find((o) => o.status === 'PENDING')!;
      const cancelledOrder = { ...pendingOrder, status: 'CANCELLED' as OrderStatus };
      orderServiceSpy.cancelOrder.and.returnValue(of(cancelledOrder));

      spyOn(window, 'confirm').and.returnValue(true);

      component.cancelOrder(pendingOrder);

      expect(orderServiceSpy.cancelOrder).toHaveBeenCalledWith(pendingOrder.id!);
      const updatedOrder = component.orders.find((o) => o.id === pendingOrder.id);
      expect(updatedOrder?.status).toBe('CANCELLED');
    });

    it('should not cancel if user declines confirmation', () => {
      fixture.detectChanges();

      const pendingOrder = component.orders.find((o) => o.status === 'PENDING')!;
      spyOn(window, 'confirm').and.returnValue(false);

      component.cancelOrder(pendingOrder);

      expect(orderServiceSpy.cancelOrder).not.toHaveBeenCalled();
    });
  });

  describe('Empty state', () => {
    it('should show empty state when no orders', () => {
      orderServiceSpy.getOrders.and.returnValue(of([]));
      fixture.detectChanges();

      expect(component.orders.length).toBe(0);
      const compiled = fixture.nativeElement;
      expect(compiled.querySelector('.empty-state')).toBeTruthy();
    });
  });
});

