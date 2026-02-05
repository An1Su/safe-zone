import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { environment } from '../../environments/environments';
import { CreateOrderRequest, Order, OrderSearchParams, OrderStats } from '../models/order.model';
import { AuthService } from './auth.service';
import { OrderService } from './order.service';

describe('OrderService', () => {
  let service: OrderService;
  let httpMock: HttpTestingController;
  let authServiceSpy: jasmine.SpyObj<AuthService>;

  const mockHeaders = { Authorization: 'Bearer test-token' };
  const apiUrl = `${environment.apiUrl}/orders`;

  const mockOrder: Order = {
    id: 'order-001',
    userId: 'user-1',
    items: [
      {
        productId: 'prod-1',
        productName: 'Test Product',
        sellerId: 'seller-1',
        price: 29.99,
        quantity: 2,
      },
    ],
    status: 'PENDING',
    totalAmount: 59.98,
    shippingAddress: {
      fullName: 'Test User',
      address: '123 Test St',
      city: 'Test City',
      phone: '555-0123',
    },
    createdAt: new Date(),
  };

  beforeEach(() => {
    const authSpy = jasmine.createSpyObj('AuthService', ['getAuthHeaders']);
    authSpy.getAuthHeaders.and.returnValue(mockHeaders);

    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [
        OrderService,
        { provide: AuthService, useValue: authSpy },
      ],
    });

    service = TestBed.inject(OrderService);
    httpMock = TestBed.inject(HttpTestingController);
    authServiceSpy = TestBed.inject(AuthService) as jasmine.SpyObj<AuthService>;
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('createOrder', () => {
    it('should create an order', () => {
      const shippingAddress: CreateOrderRequest = {
        fullName: 'Test User',
        address: '123 Test St',
        city: 'Test City',
        phone: '555-0123',
      };

      service.createOrder(shippingAddress).subscribe((order) => {
        expect(order).toEqual(mockOrder);
      });

      const req = httpMock.expectOne(apiUrl);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(shippingAddress);
      req.flush(mockOrder);
    });
  });

  describe('getOrders', () => {
    it('should get user orders', () => {
      const mockOrders: Order[] = [mockOrder];

      service.getOrders().subscribe((orders) => {
        expect(orders).toEqual(mockOrders);
      });

      const req = httpMock.expectOne(apiUrl);
      expect(req.request.method).toBe('GET');
      req.flush(mockOrders);
    });
  });

  describe('getMyOrders', () => {
    it('should be an alias for getOrders', () => {
      const mockOrders: Order[] = [mockOrder];

      service.getMyOrders().subscribe((orders) => {
        expect(orders).toEqual(mockOrders);
      });

      const req = httpMock.expectOne(apiUrl);
      expect(req.request.method).toBe('GET');
      req.flush(mockOrders);
    });
  });

  describe('getOrderById', () => {
    it('should get a specific order by ID', () => {
      const orderId = 'order-001';

      service.getOrderById(orderId).subscribe((order) => {
        expect(order).toEqual(mockOrder);
      });

      const req = httpMock.expectOne(`${apiUrl}/${orderId}`);
      expect(req.request.method).toBe('GET');
      req.flush(mockOrder);
    });
  });

  describe('cancelOrder', () => {
    it('should cancel an order', () => {
      const orderId = 'order-001';
      const cancelledOrder = { ...mockOrder, status: 'CANCELLED' as const };

      service.cancelOrder(orderId).subscribe((order) => {
        expect(order.status).toBe('CANCELLED');
      });

      const req = httpMock.expectOne(`${apiUrl}/${orderId}/cancel`);
      expect(req.request.method).toBe('PUT');
      req.flush(cancelledOrder);
    });
  });

  describe('deleteOrder', () => {
    it('should delete an order', () => {
      const orderId = 'order-001';

      service.deleteOrder(orderId).subscribe();

      const req = httpMock.expectOne(`${apiUrl}/${orderId}`);
      expect(req.request.method).toBe('DELETE');
      req.flush(null);
    });
  });

  describe('redoOrder', () => {
    it('should redo a cancelled order', () => {
      const orderId = 'order-001';
      const newOrder = { ...mockOrder, id: 'order-002' };

      service.redoOrder(orderId).subscribe((order) => {
        expect(order.id).toBe('order-002');
      });

      const req = httpMock.expectOne(`${apiUrl}/${orderId}/redo`);
      expect(req.request.method).toBe('POST');
      req.flush(newOrder);
    });
  });

  describe('searchOrders', () => {
    it('should search orders with all params', () => {
      const params: OrderSearchParams = {
        q: 'test',
        status: 'PENDING',
        dateFrom: '2024-01-01',
        dateTo: '2024-12-31',
      };
      const mockOrders: Order[] = [mockOrder];

      service.searchOrders(params).subscribe((orders) => {
        expect(orders).toEqual(mockOrders);
      });

      const req = httpMock.expectOne((request) => request.url === `${apiUrl}/search`);
      expect(req.request.method).toBe('GET');
      expect(req.request.params.get('q')).toBe('test');
      expect(req.request.params.get('status')).toBe('PENDING');
      expect(req.request.params.get('dateFrom')).toBe('2024-01-01');
      expect(req.request.params.get('dateTo')).toBe('2024-12-31');
      req.flush(mockOrders);
    });

    it('should search orders with partial params', () => {
      const params: OrderSearchParams = { q: 'test' };
      const mockOrders: Order[] = [mockOrder];

      service.searchOrders(params).subscribe((orders) => {
        expect(orders).toEqual(mockOrders);
      });

      const req = httpMock.expectOne((request) => request.url === `${apiUrl}/search`);
      expect(req.request.params.get('q')).toBe('test');
      expect(req.request.params.get('status')).toBeNull();
      req.flush(mockOrders);
    });
  });

  describe('getUserStats', () => {
    it('should get user order statistics', () => {
      const mockStats: OrderStats = {
        totalSpent: 500.0,
        orderCount: 10,
        ordersByStatus: {
          PENDING: 2,
          READY_FOR_DELIVERY: 0,
          SHIPPED: 0,
          DELIVERED: 8,
          CANCELLED: 0,
        },
      };

      service.getUserStats().subscribe((stats) => {
        expect(stats).toEqual(mockStats);
      });

      const req = httpMock.expectOne(`${apiUrl}/stats/user`);
      expect(req.request.method).toBe('GET');
      req.flush(mockStats);
    });
  });

  describe('Seller endpoints', () => {
    it('should get seller orders', () => {
      const mockOrders: Order[] = [mockOrder];

      service.getSellerOrders().subscribe((orders) => {
        expect(orders).toEqual(mockOrders);
      });

      const req = httpMock.expectOne(`${apiUrl}/seller`);
      expect(req.request.method).toBe('GET');
      req.flush(mockOrders);
    });

    it('should get seller order by ID', () => {
      const orderId = 'order-001';

      service.getSellerOrderById(orderId).subscribe((order) => {
        expect(order).toEqual(mockOrder);
      });

      const req = httpMock.expectOne(`${apiUrl}/seller/${orderId}`);
      expect(req.request.method).toBe('GET');
      req.flush(mockOrder);
    });

    it('should update order status', () => {
      const orderId = 'order-001';
      const updatedOrder = { ...mockOrder, status: 'SHIPPED' as const };

      service.updateOrderStatus(orderId, 'SHIPPED').subscribe((order) => {
        expect(order.status).toBe('SHIPPED');
      });

      const req = httpMock.expectOne((request) => request.url === `${apiUrl}/${orderId}/status`);
      expect(req.request.method).toBe('PUT');
      expect(req.request.params.get('status')).toBe('SHIPPED');
      req.flush(updatedOrder);
    });

    it('should search seller orders', () => {
      const params: OrderSearchParams = { status: 'PENDING' };
      const mockOrders: Order[] = [mockOrder];

      service.searchSellerOrders(params).subscribe((orders) => {
        expect(orders).toEqual(mockOrders);
      });

      const req = httpMock.expectOne((request) => request.url === `${apiUrl}/seller/search`);
      expect(req.request.method).toBe('GET');
      expect(req.request.params.get('status')).toBe('PENDING');
      req.flush(mockOrders);
    });
  });
});

