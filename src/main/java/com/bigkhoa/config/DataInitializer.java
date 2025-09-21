package com.bigkhoa.config;

import com.bigkhoa.model.Laptop;
import com.bigkhoa.model.LaptopImage;
import com.bigkhoa.repository.LaptopRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DataInitializer implements CommandLineRunner {

    private final LaptopRepository laptopRepository;

    public DataInitializer(LaptopRepository laptopRepository) {
        this.laptopRepository = laptopRepository;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        // Laptop #1
        Laptop lap1 = laptopRepository.findById(1L)
            .orElseThrow(() -> new IllegalStateException("Không tìm thấy Laptop #1"));
        if (lap1.getImages().isEmpty()) {
            lap1.getImages().add(new LaptopImage(lap1, "/images/acer_nitro5.jpg"));
            lap1.getImages().add(new LaptopImage(lap1, "/images/acer_swift5.jpg"));
            lap1.getImages().add(new LaptopImage(lap1, "/images/acer_nitro5.jpg"));
            lap1.getImages().add(new LaptopImage(lap1, "/images/acer_swift5.jpg"));
            laptopRepository.save(lap1);
        }

        // Laptop #2
        Laptop lap2 = laptopRepository.findById(2L)
            .orElseThrow(() -> new IllegalStateException("Không tìm thấy Laptop #2"));
        if (lap2.getImages().isEmpty()) {
            lap2.getImages().add(new LaptopImage(lap2, "/images/acer_swift5.jpg"));
            lap2.getImages().add(new LaptopImage(lap2, "/images/acer_nitro5.jpg"));
            lap2.getImages().add(new LaptopImage(lap2, "/images/asus_rog.jpg"));
            lap2.getImages().add(new LaptopImage(lap2, "/images/acer_nitro5.jpg"));
            laptopRepository.save(lap2);
        }

        // Laptop #3
        Laptop lap3 = laptopRepository.findById(3L)
            .orElseThrow(() -> new IllegalStateException("Không tìm thấy Laptop #3"));
        if (lap3.getImages().isEmpty()) {
            lap3.getImages().add(new LaptopImage(lap3, "/images/acer_nitro5.jpg"));
            lap3.getImages().add(new LaptopImage(lap3, "/images/asus_rog.jpg"));
            lap3.getImages().add(new LaptopImage(lap3, "/images/asus_rog.jpg"));
            lap3.getImages().add(new LaptopImage(lap3, "/images/dell_xps13.jpg"));
            laptopRepository.save(lap3);
        }

        // Laptop #4
        Laptop lap4 = laptopRepository.findById(4L)
            .orElseThrow(() -> new IllegalStateException("Không tìm thấy Laptop #4"));
        if (lap4.getImages().isEmpty()) {
            lap4.getImages().add(new LaptopImage(lap4, "/images/dell_xps13.jpg"));
            lap4.getImages().add(new LaptopImage(lap4, "/images/dell_xps13.jpg"));
            lap4.getImages().add(new LaptopImage(lap4, "/images/dell_xps13.jpg"));
            lap4.getImages().add(new LaptopImage(lap4, "/images/dell_xps13.jpg"));
            laptopRepository.save(lap4);
        }

        // Laptop #5
        Laptop lap5 = laptopRepository.findById(5L)
            .orElseThrow(() -> new IllegalStateException("Không tìm thấy Laptop #5"));
        if (lap5.getImages().isEmpty()) {
            lap5.getImages().add(new LaptopImage(lap5, "/images/lenovo_x1.jpg"));
            lap5.getImages().add(new LaptopImage(lap5, "/images/lenovo1.jpg"));
            lap5.getImages().add(new LaptopImage(lap5, "/images/lenovo_x1.jpg"));
            lap5.getImages().add(new LaptopImage(lap5, "/images/lenovo_x1.jpg"));
            laptopRepository.save(lap5);
        }

        // Laptop #6
        Laptop lap6 = laptopRepository.findById(6L)
            .orElseThrow(() -> new IllegalStateException("Không tìm thấy Laptop #6"));
        if (lap6.getImages().isEmpty()) {
            lap6.getImages().add(new LaptopImage(lap6, "/images/macobook_air_m1.jpg"));
            lap6.getImages().add(new LaptopImage(lap6, "/images/macobook_air_m1_2.jpg"));
            lap6.getImages().add(new LaptopImage(lap6, "/images/macobook_air_m1_3.jpg"));
            lap6.getImages().add(new LaptopImage(lap6, "/images/lenovo_x1.jpg"));
            laptopRepository.save(lap6);
        }

        // Laptop #7
        Laptop lap7 = laptopRepository.findById(7L)
            .orElseThrow(() -> new IllegalStateException("Không tìm thấy Laptop #7"));
        if (lap7.getImages().isEmpty()) {
            lap7.getImages().add(new LaptopImage(lap7, "/images/msi_gf63.jpg"));
            lap7.getImages().add(new LaptopImage(lap7, "/images/msi_prestige.jpg"));
            lap7.getImages().add(new LaptopImage(lap7, "/images/msi_gf63_detail.jpg"));
            lap7.getImages().add(new LaptopImage(lap7, "/images/msi_prestige_detail.jpg"));
            laptopRepository.save(lap7);
        }

        // Laptop #8
        Laptop lap8 = laptopRepository.findById(8L)
            .orElseThrow(() -> new IllegalStateException("Không tìm thấy Laptop #8"));
        if (lap8.getImages().isEmpty()) {
            lap8.getImages().add(new LaptopImage(lap8, "/images/surface_laptop.jpg"));
            lap8.getImages().add(new LaptopImage(lap8, "/images/surface_laptop_2.jpg"));
            lap8.getImages().add(new LaptopImage(lap8, "/images/surface_laptop_3.jpg"));
            lap8.getImages().add(new LaptopImage(lap8, "/images/surface_laptop_detail.jpg"));
            laptopRepository.save(lap8);
        }

        // Laptop #9
        Laptop lap9 = laptopRepository.findById(9L)
            .orElseThrow(() -> new IllegalStateException("Không tìm thấy Laptop #9"));
        if (lap9.getImages().isEmpty()) {
            lap9.getImages().add(new LaptopImage(lap9, "/images/acer_nitro5.jpg"));
            lap9.getImages().add(new LaptopImage(lap9, "/images/acer_swift5.jpg"));
            lap9.getImages().add(new LaptopImage(lap9, "/images/acer_spin3.jpg"));
            lap9.getImages().add(new LaptopImage(lap9, "/images/acer_nitro5_detail.jpg"));
            laptopRepository.save(lap9);
        }

        // Laptop #10
        Laptop lap10 = laptopRepository.findById(10L)
            .orElseThrow(() -> new IllegalStateException("Không tìm thấy Laptop #10"));
        if (lap10.getImages().isEmpty()) {
            lap10.getImages().add(new LaptopImage(lap10, "/images/asus_rog.jpg"));
            lap10.getImages().add(new LaptopImage(lap10, "/images/asus_vivobook14.jpg"));
            lap10.getImages().add(new LaptopImage(lap10, "/images/asus_rog_detail.jpg"));
            lap10.getImages().add(new LaptopImage(lap10, "/images/asus_vivobook14_detail.jpg"));
            laptopRepository.save(lap10);
        }

        // Laptop #11
        Laptop lap11 = laptopRepository.findById(11L)
            .orElseThrow(() -> new IllegalStateException("Không tìm thấy Laptop #11"));
        if (lap11.getImages().isEmpty()) {
            lap11.getImages().add(new LaptopImage(lap11, "/images/dell_xps13.jpg"));
            lap11.getImages().add(new LaptopImage(lap11, "/images/dell_xps15.jpg"));
            lap11.getImages().add(new LaptopImage(lap11, "/images/dell1.jpg"));
            lap11.getImages().add(new LaptopImage(lap11, "/images/dell_xps13_detail.jpg"));
            laptopRepository.save(lap11);
        }

        // Laptop #12
        Laptop lap12 = laptopRepository.findById(12L)
            .orElseThrow(() -> new IllegalStateException("Không tìm thấy Laptop #12"));
        if (lap12.getImages().isEmpty()) {
            lap12.getImages().add(new LaptopImage(lap12, "/images/hp_envy13.jpg"));
            lap12.getImages().add(new LaptopImage(lap12, "/images/hp_spectre.jpg"));
            lap12.getImages().add(new LaptopImage(lap12, "/images/hp1.jpg"));
            lap12.getImages().add(new LaptopImage(lap12, "/images/hp_envy13_detail.jpg"));
            laptopRepository.save(lap12);
        }

        // Laptop #13
        Laptop lap13 = laptopRepository.findById(13L)
            .orElseThrow(() -> new IllegalStateException("Không tìm thấy Laptop #13"));
        if (lap13.getImages().isEmpty()) {
            lap13.getImages().add(new LaptopImage(lap13, "/images/lenovo_x1.jpg"));
            lap13.getImages().add(new LaptopImage(lap13, "/images/lenovo1.jpg"));
            lap13.getImages().add(new LaptopImage(lap13, "/images/lenovo_x1_detail.jpg"));
            lap13.getImages().add(new LaptopImage(lap13, "/images/lenovo1_detail.jpg"));
            laptopRepository.save(lap13);
        }

        // Laptop #14
        Laptop lap14 = laptopRepository.findById(14L)
            .orElseThrow(() -> new IllegalStateException("Không tìm thấy Laptop #14"));
        if (lap14.getImages().isEmpty()) {
            lap14.getImages().add(new LaptopImage(lap14, "/images/msi_gf63.jpg"));
            lap14.getImages().add(new LaptopImage(lap14, "/images/msi_gf63.jpg"));
            lap14.getImages().add(new LaptopImage(lap14, "/images/acer_nitro5.jpg"));
            lap14.getImages().add(new LaptopImage(lap14, "/images/acer_nitro5.jpg"));
            laptopRepository.save(lap14);
        }

        // Laptop #15
        Laptop lap15 = laptopRepository.findById(15L)
            .orElseThrow(() -> new IllegalStateException("Không tìm thấy Laptop #15"));
        if (lap15.getImages().isEmpty()) {
            lap15.getImages().add(new LaptopImage(lap15, "/images/msi_gf63.jpg"));
            lap15.getImages().add(new LaptopImage(lap15, "/images/acer_nitro5.jpg"));
            lap15.getImages().add(new LaptopImage(lap15, "/images/msi_gf63.jpg"));
            lap15.getImages().add(new LaptopImage(lap15, "/images/lenovo_x1.jpg"));
            laptopRepository.save(lap15);
        }

        // Laptop #16
        Laptop lap16 = laptopRepository.findById(16L)
            .orElseThrow(() -> new IllegalStateException("Không tìm thấy Laptop #16"));
        if (lap16.getImages().isEmpty()) {
            lap16.getImages().add(new LaptopImage(lap16, "/images/surface_laptop.jpg"));
            lap16.getImages().add(new LaptopImage(lap16, "/images/surface_laptop.jpg"));
            lap16.getImages().add(new LaptopImage(lap16, "/images/surface_laptop.jpg"));
            lap16.getImages().add(new LaptopImage(lap16, "/images/surface_laptop.jpg"));
            laptopRepository.save(lap16);
        }
    }
}
