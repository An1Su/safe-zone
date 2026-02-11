import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { map, Observable } from 'rxjs';
import { environment } from '../../environments/environments';
import { Product, ProductCategory } from '../models/ecommerce.model';
import { AuthService } from './auth.service';

export interface ProductSearchParams {
  query?: string;
  category?: ProductCategory;
  minPrice?: number;
  maxPrice?: number;
  sortBy?: 'price_asc' | 'price_desc' | 'name' | 'newest';
  page?: number;
  limit?: number;
}

export interface ProductSearchResult {
  products: Product[];
  total: number;
  page: number;
  totalPages: number;
}

@Injectable({
  providedIn: 'root',
})
export class ProductService {
  private apiUrl = `${environment.apiUrl}/products`;

  constructor(private http: HttpClient, private authService: AuthService) {}

  getAllProducts(): Observable<Product[]> {
    return this.http.get<Product[]>(this.apiUrl);
  }

  /**
   * Search and filter products with pagination
   * Note: Currently filtering is done client-side until backend search endpoint is added
   */
  searchProducts(params: ProductSearchParams): Observable<ProductSearchResult> {
    return this.getAllProducts().pipe(
      map(products => {
        let filtered = [...products];

        // Text search
        if (params.query?.trim()) {
          const query = params.query.toLowerCase();
          filtered = filtered.filter(p =>
            p.name.toLowerCase().includes(query) ||
            p.description.toLowerCase().includes(query)
          );
        }

        // Category filter
        if (params.category) {
          filtered = filtered.filter(p => p.category === params.category);
        }

        // Price range filter
        if (params.minPrice !== undefined) {
          filtered = filtered.filter(p => p.price >= params.minPrice!);
        }
        if (params.maxPrice !== undefined) {
          filtered = filtered.filter(p => p.price <= params.maxPrice!);
        }

        // Sorting
        if (params.sortBy) {
          switch (params.sortBy) {
            case 'price_asc':
              filtered.sort((a, b) => a.price - b.price);
              break;
            case 'price_desc':
              filtered.sort((a, b) => b.price - a.price);
              break;
            case 'name':
              filtered.sort((a, b) => a.name.localeCompare(b.name));
              break;
            case 'newest':
              // Assuming newer products have higher/later IDs
              filtered.reverse();
              break;
          }
        }

        // Pagination
        const page = params.page || 1;
        const limit = params.limit || 6;
        const total = filtered.length;
        const totalPages = Math.ceil(total / limit);
        const start = (page - 1) * limit;
        const paginatedProducts = filtered.slice(start, start + limit);

        return {
          products: paginatedProducts,
          total,
          page,
          totalPages
        };
      })
    );
  }

  /**
   * Get all unique categories from products
   */
  getCategories(): ProductCategory[] {
    return ['Face', 'Eyes', 'Lips'];
  }

  getProductById(id: string): Observable<Product> {
    return this.http.get<Product>(`${this.apiUrl}/${id}`);
  }

  getMyProducts(): Observable<Product[]> {
    return this.http.get<Product[]>(`${this.apiUrl}/my-products`, {
      headers: this.authService.getAuthHeaders(),
      withCredentials: true,
    });
  }

  createProduct(product: Product): Observable<Product> {
    return this.http.post<Product>(this.apiUrl, product, {
      headers: this.authService.getAuthHeaders(),
      withCredentials: true,
    });
  }

  updateProduct(id: string, product: Product): Observable<Product> {
    return this.http.put<Product>(`${this.apiUrl}/${id}`, product, {
      headers: this.authService.getAuthHeaders(),
      withCredentials: true,
    });
  }

  deleteProduct(id: string): Observable<any> {
    return this.http.delete(`${this.apiUrl}/${id}`, {
      headers: this.authService.getAuthHeaders(),
      withCredentials: true,
    });
  }
}
