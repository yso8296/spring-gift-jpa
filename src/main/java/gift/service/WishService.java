package gift.service;

import gift.common.dto.PageResponse;
import gift.common.exception.ExistWishException;
import gift.common.exception.ProductNotFoundException;
import gift.common.exception.UserNotFoundException;
import gift.common.exception.WishNotFoundException;
import gift.model.product.Product;
import gift.model.user.User;
import gift.model.wish.Wish;
import gift.model.wish.WishRequest;
import gift.model.wish.WishResponse;
import gift.repository.ProductRepository;
import gift.repository.UserRepository;
import gift.repository.WishRepository;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class WishService {

    private final WishRepository wishRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public WishService(WishRepository wishRepository, ProductRepository productRepository,
        UserRepository userRepository) {
        this.wishRepository = wishRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
    }


    public PageResponse<WishResponse> findAllWish(Long userId, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page - 1, size, Sort.by("id").descending());
        User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
        Page<Wish> wishList = wishRepository.findByUserId(userId, pageRequest);

        List<WishResponse> wishResponses = wishList.getContent().stream()
            .map(wish -> WishResponse.from(wish,
                productRepository.findById(wish.getProduct().getId()).orElseThrow(
                    ProductNotFoundException::new)))
            .toList();
        int totalCount = (int) wishList.getTotalElements();
        return new PageResponse<>(wishResponses, page, size, totalCount);
    }

    @Transactional
    public void addWistList(Long userId, WishRequest wishRequest) {
        User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);

        Product product = productRepository.findById(wishRequest.productId())
            .orElseThrow(ProductNotFoundException::new);

        if (wishRepository.existsByProductIdAndUserId(product.getId(), userId)) {
            throw new ExistWishException();
        }

        wishRepository.save(wishRequest.toEntity(user, product, wishRequest.count()));
    }

    @Transactional
    public void updateWishList(Long userId, WishRequest wishRequest) {
        User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);

        Product product = productRepository.findById(wishRequest.productId())
            .orElseThrow(ProductNotFoundException::new);

        Wish wish = wishRepository.findByProductIdAndUserId(wishRequest.productId(), userId)
            .orElseThrow(WishNotFoundException::new);

        if (wishRequest.count() == 0) {
            deleteWishList(userId, wishRequest.productId());
        } else {
            wish.updateWish(wishRequest.count());
        }
    }

    @Transactional
    public void deleteWishList(Long userId, Long productId) {
        User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);

        Product product = productRepository.findById(productId)
            .orElseThrow(ProductNotFoundException::new);

        if (!wishRepository.existsByProductIdAndUserId(productId, userId)) {
            throw new WishNotFoundException();
        }

        wishRepository.deleteByProductIdAndUserId(productId, userId);
    }
}
