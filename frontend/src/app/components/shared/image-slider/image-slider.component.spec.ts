import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ImageSliderComponent } from './image-slider.component';

describe('ImageSliderComponent', () => {
  let component: ImageSliderComponent;
  let fixture: ComponentFixture<ImageSliderComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ImageSliderComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(ImageSliderComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    fixture.detectChanges();
    expect(component).toBeTruthy();
  });

  it('should have default values', () => {
    expect(component.images).toEqual([]);
    expect(component.alt).toBe('Product image');
    expect(component.showDots).toBe(true);
    expect(component.showCounter).toBe(false);
    expect(component.currentIndex).toBe(0);
  });

  describe('nextImage', () => {
    it('should go to next image', () => {
      component.images = ['img1.jpg', 'img2.jpg', 'img3.jpg'];
      component.currentIndex = 0;
      const event = new Event('click');
      spyOn(event, 'stopPropagation');

      component.nextImage(event);

      expect(event.stopPropagation).toHaveBeenCalled();
      expect(component.currentIndex).toBe(1);
    });

    it('should wrap to first image when at the end', () => {
      component.images = ['img1.jpg', 'img2.jpg', 'img3.jpg'];
      component.currentIndex = 2;
      const event = new Event('click');

      component.nextImage(event);

      expect(component.currentIndex).toBe(0);
    });

    it('should not change index when images array is empty', () => {
      component.images = [];
      component.currentIndex = 0;
      const event = new Event('click');

      component.nextImage(event);

      expect(component.currentIndex).toBe(0);
    });
  });

  describe('prevImage', () => {
    it('should go to previous image', () => {
      component.images = ['img1.jpg', 'img2.jpg', 'img3.jpg'];
      component.currentIndex = 2;
      const event = new Event('click');
      spyOn(event, 'stopPropagation');

      component.prevImage(event);

      expect(event.stopPropagation).toHaveBeenCalled();
      expect(component.currentIndex).toBe(1);
    });

    it('should wrap to last image when at the beginning', () => {
      component.images = ['img1.jpg', 'img2.jpg', 'img3.jpg'];
      component.currentIndex = 0;
      const event = new Event('click');

      component.prevImage(event);

      expect(component.currentIndex).toBe(2);
    });

    it('should not change index when images array is empty', () => {
      component.images = [];
      component.currentIndex = 0;
      const event = new Event('click');

      component.prevImage(event);

      expect(component.currentIndex).toBe(0);
    });
  });

  describe('goToImage', () => {
    it('should go to specific image index', () => {
      component.images = ['img1.jpg', 'img2.jpg', 'img3.jpg'];
      component.currentIndex = 0;
      const event = new Event('click');
      spyOn(event, 'stopPropagation');

      component.goToImage(2, event);

      expect(event.stopPropagation).toHaveBeenCalled();
      expect(component.currentIndex).toBe(2);
    });

    it('should set currentIndex to 0', () => {
      component.images = ['img1.jpg', 'img2.jpg', 'img3.jpg'];
      component.currentIndex = 2;
      const event = new Event('click');

      component.goToImage(0, event);

      expect(component.currentIndex).toBe(0);
    });
  });
});

