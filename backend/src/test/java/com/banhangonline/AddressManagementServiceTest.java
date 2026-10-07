package com.banhangonline;

import com.banhangonline.address.dto.AddressRequest;
import com.banhangonline.address.entity.UserAddress;
import com.banhangonline.address.repository.UserAddressRepository;
import com.banhangonline.address.service.AddressManagementService;
import com.banhangonline.common.exception.ApiException;
import com.banhangonline.user.entity.User;
import com.banhangonline.user.repository.UserRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AddressManagementServiceTest {
    private final UserAddressRepository addresses = mock(UserAddressRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final AddressManagementService service = new AddressManagementService(addresses, users);

    @Test
    void savesSelectedCoordinatesOnCustomerAddress() {
        User user = new User();
        user.setId(7L);
        when(users.getReferenceById(7L)).thenReturn(user);
        when(addresses.findByUserIdOrderByDefaultAddressDescIdDesc(7L)).thenReturn(List.of());
        when(addresses.save(any(UserAddress.class))).thenAnswer(call -> {
            UserAddress address = call.getArgument(0);
            address.setId(11L);
            return address;
        });

        var response = service.create(7L, new AddressRequest("Người nhận", "0912345678",
                "Hà Nội", "Ba Đình", "Phúc Xá", "1 Phố Mẫu",
                new BigDecimal("21.0345000"), new BigDecimal("105.8123000")));

        assertThat(response.latitude()).isEqualByComparingTo("21.0345000");
        assertThat(response.longitude()).isEqualByComparingTo("105.8123000");
        assertThat(response.isDefault()).isTrue();
    }

    @Test
    void rejectsAddressWithOnlyOneCoordinate() {
        User user = new User();
        user.setId(7L);
        when(users.getReferenceById(7L)).thenReturn(user);

        assertThatThrownBy(() -> service.create(7L, new AddressRequest("Người nhận", "0912345678",
                "Hà Nội", "Ba Đình", "Phúc Xá", "1 Phố Mẫu",
                new BigDecimal("21.0345000"), null)))
                .isInstanceOf(ApiException.class)
                .extracting("code").isEqualTo("VALIDATION_ERROR");
        verify(addresses, never()).save(any(UserAddress.class));
    }
}
