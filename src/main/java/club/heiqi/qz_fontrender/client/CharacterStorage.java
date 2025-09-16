package club.heiqi.qz_fontrender.client;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

/**
 * 高速缓存类
 */
public class CharacterStorage {
    /**
     * 根据使用的频率自动舍弃旧的 容量最大 10,240 字符
     */
    public Cache<Integer, Object> storage = CacheBuilder.newBuilder()
            .maximumSize(10240).build();
}
