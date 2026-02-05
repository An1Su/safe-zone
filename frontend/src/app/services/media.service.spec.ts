import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { environment } from '../../environments/environments';
import { Avatar, Media } from '../models/ecommerce.model';
import { AuthService } from './auth.service';
import { MediaService } from './media.service';

describe('MediaService', () => {
  let service: MediaService;
  let httpMock: HttpTestingController;
  let authServiceSpy: jasmine.SpyObj<AuthService>;

  const apiUrl = `${environment.apiUrl}/media`;

  const mockMedia: Media = {
    id: 'media-001',
    imagePath: '/images/product-1.jpg',
    productId: 'prod-001',
    fileName: 'product-1.jpg',
    contentType: 'image/jpeg',
    fileSize: 1024,
  };

  const mockAvatar: Avatar = {
    id: 'avatar-001',
    imagePath: '/avatars/user-1.jpg',
    userId: 'user-001',
    fileName: 'user-1.jpg',
    contentType: 'image/jpeg',
    fileSize: 512,
  };

  beforeEach(() => {
    const authSpy = jasmine.createSpyObj('AuthService', ['getAuthHeaders']);
    authSpy.getAuthHeaders.and.returnValue({ Authorization: 'Bearer test-token' });

    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [
        MediaService,
        { provide: AuthService, useValue: authSpy },
      ],
    });

    service = TestBed.inject(MediaService);
    httpMock = TestBed.inject(HttpTestingController);
    authServiceSpy = TestBed.inject(AuthService) as jasmine.SpyObj<AuthService>;

    // Mock localStorage
    spyOn(localStorage, 'getItem').and.returnValue('test-token');
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('uploadMedia', () => {
    it('should upload media for a product', () => {
      const file = new File(['test'], 'test.jpg', { type: 'image/jpeg' });
      const productId = 'prod-001';

      service.uploadMedia(file, productId).subscribe((media) => {
        expect(media).toEqual(mockMedia);
      });

      const req = httpMock.expectOne(`${apiUrl}/upload/${productId}`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body instanceof FormData).toBeTrue();
      req.flush(mockMedia);
    });
  });

  describe('getMediaByProduct', () => {
    it('should get media for a product', () => {
      const productId = 'prod-001';
      const mockMediaList: Media[] = [mockMedia];

      service.getMediaByProduct(productId).subscribe((mediaList) => {
        expect(mediaList).toEqual(mockMediaList);
      });

      const req = httpMock.expectOne(`${apiUrl}/product/${productId}`);
      expect(req.request.method).toBe('GET');
      req.flush(mockMediaList);
    });
  });

  describe('getMediaFile', () => {
    it('should return the correct media file URL', () => {
      const mediaId = 'media-001';
      const url = service.getMediaFile(mediaId);
      expect(url).toBe(`${apiUrl}/file/${mediaId}`);
    });
  });

  describe('deleteMedia', () => {
    it('should delete media by ID', () => {
      const mediaId = 'media-001';

      service.deleteMedia(mediaId).subscribe();

      const req = httpMock.expectOne(`${apiUrl}/${mediaId}`);
      expect(req.request.method).toBe('DELETE');
      req.flush({});
    });
  });

  describe('deleteMediaByProduct', () => {
    it('should delete all media for a product', () => {
      const productId = 'prod-001';

      service.deleteMediaByProduct(productId).subscribe();

      const req = httpMock.expectOne(`${apiUrl}/product/${productId}`);
      expect(req.request.method).toBe('DELETE');
      req.flush({});
    });
  });

  describe('uploadAvatar', () => {
    it('should upload an avatar', () => {
      const file = new File(['test'], 'avatar.jpg', { type: 'image/jpeg' });

      service.uploadAvatar(file).subscribe((avatar) => {
        expect(avatar).toEqual(mockAvatar);
      });

      const req = httpMock.expectOne(`${apiUrl}/avatar/upload`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body instanceof FormData).toBeTrue();
      req.flush(mockAvatar);
    });
  });

  describe('getAvatarByUserId', () => {
    it('should get avatar by user ID', () => {
      const userId = 'user-001';

      service.getAvatarByUserId(userId).subscribe((avatar) => {
        expect(avatar).toEqual(mockAvatar);
      });

      const req = httpMock.expectOne(`${apiUrl}/avatar/user/${userId}`);
      expect(req.request.method).toBe('GET');
      req.flush(mockAvatar);
    });
  });

  describe('getAvatarFileUrl', () => {
    it('should return the correct avatar file URL', () => {
      const avatarId = 'avatar-001';
      const url = service.getAvatarFileUrl(avatarId);
      expect(url).toBe(`${apiUrl}/avatar/file/${avatarId}`);
    });
  });

  describe('deleteAvatar', () => {
    it('should delete the current user avatar', () => {
      service.deleteAvatar().subscribe();

      const req = httpMock.expectOne(`${apiUrl}/avatar`);
      expect(req.request.method).toBe('DELETE');
      req.flush({});
    });
  });
});

