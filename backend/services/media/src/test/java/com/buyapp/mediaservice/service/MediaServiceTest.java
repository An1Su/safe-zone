package com.buyapp.mediaservice.service;

import com.buyapp.common.event.MediaEvent;
import com.buyapp.mediaservice.model.Media;
import com.buyapp.mediaservice.repository.MediaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MediaServiceTest {

    @Mock
    private MediaRepository mediaRepository;

    @Mock
    private MediaEventProducer mediaEventProducer;

    private MediaService mediaService;

    @BeforeEach
    void setUp() {
        mediaService = new MediaService(mediaRepository, mediaEventProducer);
    }

    @Test
    void whenMediaUploaded_thenPublishesMediaEvent() {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test-image.jpg",
                "image/jpeg",
                "test image content".getBytes());

        String productId = "prod-123";

        UserDetails userDetails = new User(
                "seller@example.com",
                "password",
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));
        Authentication auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

        Media savedMedia = new Media();
        savedMedia.setId("media-456");
        savedMedia.setProductId(productId);
        savedMedia.setFileName("test-image.jpg");

        when(mediaRepository.save(any(Media.class))).thenReturn(savedMedia);

        // Act
        Media result = mediaService.uploadMedia(file, productId, auth);

        // Assert: verify event was published
        ArgumentCaptor<MediaEvent> eventCaptor = ArgumentCaptor.forClass(MediaEvent.class);
        verify(mediaEventProducer).sendMediaEvent(eventCaptor.capture());

        MediaEvent capturedEvent = eventCaptor.getValue();
        assertEquals("IMAGE_UPLOADED", capturedEvent.getEventType());
        assertEquals("media-456", capturedEvent.getMediaId());
        assertEquals(productId, capturedEvent.getProductId());
        assertEquals("test-image.jpg", capturedEvent.getFileName());
        assertEquals("image/jpeg", capturedEvent.getContentType());
        assertEquals("seller@example.com", capturedEvent.getUploadedBy());

        // Verify media was saved
        assertNotNull(result);
        assertEquals("media-456", result.getId());
    }

    @Test
    void whenMediaUploadFails_thenNoEventPublished() {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.jpg",
                "image/jpeg",
                new byte[0] // Empty file
        );

        UserDetails userDetails = new User("user@example.com", "pass", Collections.emptyList());
        Authentication auth = new UsernamePasswordAuthenticationToken(userDetails, null);

        when(mediaRepository.save(any(Media.class))).thenThrow(new RuntimeException("Database error"));

        // Act & Assert: should throw exception
        assertThrows(Exception.class, () -> mediaService.uploadMedia(file, "prod-123", auth));

        // Verify no event was published on failure
        verify(mediaEventProducer, never()).sendMediaEvent(any());
    }
}
