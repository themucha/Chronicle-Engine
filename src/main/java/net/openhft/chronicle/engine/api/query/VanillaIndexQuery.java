package net.openhft.chronicle.engine.api.query;

import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.annotation.UsedViaReflection;
import net.openhft.chronicle.engine.map.QueueObjectSubscription;
import net.openhft.chronicle.wire.AbstractMarshallable;
import net.openhft.chronicle.wire.Demarshallable;
import net.openhft.chronicle.wire.WireIn;
import net.openhft.compiler.CompilerUtils;
import net.openhft.lang.model.DataValueGenerator;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;

/**
 * @author Rob Austin.
 */
public class VanillaIndexQuery<V> extends AbstractMarshallable implements Demarshallable,
        IndexQuery<V> {

    private static final Logger LOG = LoggerFactory.getLogger(QueueObjectSubscription.class);
    private Class<V> valueClass;
    private String select;
    private String eventName;
    private long from;

    public VanillaIndexQuery() {
    }


    @UsedViaReflection
    public VanillaIndexQuery(@NotNull WireIn wire) {
        readMarshallable(wire);
    }

    public Class<V> valueClass() {
        return valueClass;
    }

    public VanillaIndexQuery valueClass(Class<V> valueClass) {
        this.valueClass = valueClass;
        return this;
    }

    public String select() {
        return select == null ? "" : select;
    }

    public String eventName() {
        return eventName;
    }

    public void eventName(String eventName) {
        this.eventName = eventName;
    }

    /**
     * @param valueClass the type of the value
     * @param select     java source
     * @return
     */
    public VanillaIndexQuery select(@NotNull Class valueClass, @NotNull String select) {
        this.select = select;
        this.valueClass = valueClass;

        // used to test-compile the predicate on the client side
        try {
            ClassCache.newInstance0(new ClassCache.TypedSelect(valueClass, select));
        } catch (Exception e) {
            LOG.error(e.getMessage() + "/n");
        }
        return this;
    }

    public long from() {
        return from;
    }

    public VanillaIndexQuery from(long from) {
        this.from = from;
        return this;
    }


    public Predicate<V> filter() {
        return ClassCache.newInstance(valueClass, select);
    }


    /**
     * ensures that the same slelect/predicate will return an existing class instance
     */
    private static class ClassCache {
        private static final ConcurrentMap<TypedSelect, Predicate> cache = new
                ConcurrentHashMap<>();
        private static AtomicLong uniqueClassId = new AtomicLong();

        private static class TypedSelect {
            private String select;
            private Class clazz;

            private TypedSelect(Class clazz, String select) {
                this.select = select;
                this.clazz = clazz;
            }
        }

        private static Predicate newInstance(final Class clazz0, final String select) {
            return cache.compute(new TypedSelect(clazz0, select), (typedSelect, predicate) ->
                    newInstance0(typedSelect));
        }

        private static <V> Predicate<V> newInstance0(TypedSelect typedSelect) {
            String clazz = typedSelect.clazz.getName();
            long classId = uniqueClassId.incrementAndGet();
            String source = "package net.openhft.chronicle.engine.api.query;\npublic class " +
                    "AutoGeneratedPredicate" + classId + " implements " +
                    "java.util.function.Predicate<" + clazz + "> {\n\tpublic " +
                    "boolean test(" + clazz + " value) " +
                    "{\n\t\treturn " +
                    typedSelect.select + ";\n\t}\n\n\tpublic String toString(){\n\t\treturn \"" +
                    typedSelect
                            .select + "\";\n\t}\n}";

            //System.out.println(source);
            LoggerFactory.getLogger(DataValueGenerator.class).info(source);
            ClassLoader classLoader = ClassCache.class.getClassLoader();
            try

            {
                Class<Predicate> clazzP = CompilerUtils.CACHED_COMPILER.loadFromJava(classLoader,
                        "net.openhft.chronicle.engine.api.query.AutoGeneratedPredicate" + classId, source);
                return clazzP.newInstance();
            } catch (Exception e) {
                throw Jvm.rethrow(e);
            }
        }

    }

    @Override
    public String toString() {
        return "VanillaMarshableQuery{" +
                "valueClass=" + valueClass +
                ", select='" + select + '\'' +
                ", eventName='" + eventName + '\'' +
                ", from=" + from +
                '}';
    }
}


