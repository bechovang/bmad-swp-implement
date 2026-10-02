package com.storagehub.repository;

import org.junit.jupiter.api.Test;
import org.springframework.data.repository.Repository;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * NFR-6 append-only contract of the data layer, checked by reflection: the
 * repository interface extends the base Repository marker (not
 * JpaRepository/PagingAndSortingRepository) and its only public method is
 * save - no update, no delete, no find can even be called. Pure JUnit, no
 * Spring context.
 */
class ActivityLogAppendOnlyTests {

    @Test
    void theOnlyExposedMethodIsSave() {
        List<String> methodNames = Arrays.stream(ActivityLogRepository.class.getMethods())
                .filter(method -> method.getDeclaringClass() != Object.class)
                .map(Method::getName)
                .toList();
        assertThat(methodNames).containsExactly("save");
    }

    @Test
    void extendsOnlyTheBaseRepositoryMarker() {
        assertThat(Arrays.asList(ActivityLogRepository.class.getInterfaces()))
                .containsExactly(Repository.class);
    }
}
