package com.ductieng.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ductieng.model.LaptopImage;
import com.ductieng.repository.LaptopRepository;

@Component
public class DataInitializer implements CommandLineRunner {

	private final LaptopRepository laptopRepository;

	public DataInitializer(LaptopRepository laptopRepository) {
		this.laptopRepository = laptopRepository;
	}

	@Override
	@Transactional
	public void run(String... args) {
		addImagesIfLaptopExists(1L, "/images/acer_nitro5.jpg", "/images/acer_swift5.jpg", "/images/acer_nitro5.jpg",
				"/images/acer_swift5.jpg");
		addImagesIfLaptopExists(2L, "/images/acer_swift5.jpg", "/images/acer_nitro5.jpg", "/images/asus_rog.jpg",
				"/images/acer_nitro5.jpg");
		addImagesIfLaptopExists(3L, "/images/acer_nitro5.jpg", "/images/asus_rog.jpg", "/images/asus_rog.jpg",
				"/images/dell_xps13.jpg");
		addImagesIfLaptopExists(4L, "/images/dell_xps13.jpg", "/images/dell_xps13.jpg", "/images/dell_xps13.jpg",
				"/images/dell_xps13.jpg");
		addImagesIfLaptopExists(5L, "/images/lenovo_x1.jpg", "/images/lenovo1.jpg", "/images/lenovo_x1.jpg",
				"/images/lenovo_x1.jpg");
		addImagesIfLaptopExists(6L, "/images/macobook_air_m1.jpg", "/images/macobook_air_m1_2.jpg",
				"/images/macobook_air_m1_3.jpg", "/images/lenovo_x1.jpg");
		addImagesIfLaptopExists(7L, "/images/msi_gf63.jpg", "/images/msi_prestige.jpg", "/images/msi_gf63_detail.jpg",
				"/images/msi_prestige_detail.jpg");
		addImagesIfLaptopExists(8L, "/images/surface_laptop.jpg", "/images/surface_laptop_2.jpg",
				"/images/surface_laptop_3.jpg", "/images/surface_laptop_detail.jpg");
		addImagesIfLaptopExists(9L, "/images/acer_nitro5.jpg", "/images/acer_swift5.jpg", "/images/acer_spin3.jpg",
				"/images/acer_nitro5_detail.jpg");
		addImagesIfLaptopExists(10L, "/images/asus_rog.jpg", "/images/asus_vivobook14.jpg",
				"/images/asus_rog_detail.jpg", "/images/asus_vivobook14_detail.jpg");
		addImagesIfLaptopExists(11L, "/images/dell_xps13.jpg", "/images/dell_xps15.jpg", "/images/dell1.jpg",
				"/images/dell_xps13_detail.jpg");
		addImagesIfLaptopExists(12L, "/images/hp_envy13.jpg", "/images/hp_spectre.jpg", "/images/hp1.jpg",
				"/images/hp_envy13_detail.jpg");
		addImagesIfLaptopExists(13L, "/images/lenovo_x1.jpg", "/images/lenovo1.jpg", "/images/lenovo_x1_detail.jpg",
				"/images/lenovo1_detail.jpg");
		addImagesIfLaptopExists(14L, "/images/msi_gf63.jpg", "/images/msi_gf63.jpg", "/images/acer_nitro5.jpg",
				"/images/acer_nitro5.jpg");
		addImagesIfLaptopExists(15L, "/images/msi_gf63.jpg", "/images/acer_nitro5.jpg", "/images/msi_gf63.jpg",
				"/images/lenovo_x1.jpg");
		addImagesIfLaptopExists(16L, "/images/surface_laptop.jpg", "/images/surface_laptop.jpg",
				"/images/surface_laptop.jpg", "/images/surface_laptop.jpg");
	}

	private void addImagesIfLaptopExists(Long laptopId, String... imagePaths) {
		laptopRepository.findById(laptopId).ifPresent(laptop -> {
			if (laptop.getImages().isEmpty()) {
				for (String path : imagePaths) {
					laptop.getImages().add(new LaptopImage(laptop, path));
				}
				laptopRepository.save(laptop);
			}
		});
	}

}
