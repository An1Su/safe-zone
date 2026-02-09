import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { SellerOrdersRedirectComponent } from './seller-orders-redirect.component';

describe('SellerOrdersRedirectComponent', () => {
  let component: SellerOrdersRedirectComponent;
  let fixture: ComponentFixture<SellerOrdersRedirectComponent>;
  let routerSpy: jasmine.SpyObj<Router>;

  beforeEach(async () => {
    routerSpy = jasmine.createSpyObj('Router', ['navigate']);

    await TestBed.configureTestingModule({
      imports: [SellerOrdersRedirectComponent],
      providers: [{ provide: Router, useValue: routerSpy }],
    }).compileComponents();

    fixture = TestBed.createComponent(SellerOrdersRedirectComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should redirect to /orders on init', () => {
    fixture.detectChanges();

    expect(routerSpy.navigate).toHaveBeenCalledWith(['/orders'], { replaceUrl: true });
  });
});
