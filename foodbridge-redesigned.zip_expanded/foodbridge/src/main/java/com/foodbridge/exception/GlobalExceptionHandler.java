package com.foodbridge.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.net.URI;

@ControllerAdvice
public class GlobalExceptionHandler {

    /** Shows the business error as a flash message on the page the user came from. */
    @ExceptionHandler(BusinessException.class)
    public String handleBusiness(BusinessException ex, HttpServletRequest request, RedirectAttributes ra) {
        ra.addFlashAttribute("error", ex.getMessage());
        String target = "/";
        String referer = request.getHeader("Referer");
        if (referer != null) {
            try {
                String path = URI.create(referer).getPath(); // path only, never an external host
                if (path != null && path.startsWith("/") && !path.startsWith("//")) {
                    target = path;
                }
            } catch (IllegalArgumentException ignored) {
                // fall back to "/"
            }
        }
        return "redirect:" + target;
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public String handleTooLarge(HttpServletRequest request, RedirectAttributes ra) {
        return handleBusiness(new BusinessException("The image is too large (maximum 5 MB)."), request, ra);
    }
}
