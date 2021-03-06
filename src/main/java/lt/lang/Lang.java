/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 KuiGang Wang
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package lt.lang;

import lt.compiler.LtBug;
import lt.lang.function.Function;

import java.lang.reflect.*;

/**
 * lang
 */
public class Lang {
        public static final String multiply = "multiply";
        public static final String divide = "divide";
        public static final String remainder = "remainder";
        public static final String add = "add";
        public static final String subtract = "subtract";
        public static final String shiftLeft = "shiftLeft";
        public static final String shiftRight = "shiftRight";
        public static final String unsignedShiftRight = "unsignedShiftRight";
        public static final String gt = "gt";
        public static final String lt = "lt";
        public static final String ge = "ge";
        public static final String le = "le";
        public static final String equal = "equal";
        public static final String notEqual = "notEqual";
        public static final String and = "and";
        public static final String xor = "xor";
        public static final String or = "or";

        private static boolean isBoxType(Class<?> type) {
                return type.equals(Integer.class) || type.equals(Short.class) || type.equals(Byte.class) || type.equals(Character.class)
                        || type.equals(Long.class) || type.equals(Boolean.class) || type.equals(Float.class) || type.equals(Double.class);
        }

        @SuppressWarnings("unused")
        public static Throwable castToThrowable(Object o) {
                if (o instanceof Throwable) return (Throwable) o;
                return new Wrapper(o);
        }

        public static Object cast(Object o, Class<?> targetType) throws Exception {
                if (targetType.isInstance(o)) return o;
                if (isBoxType(targetType)) {
                        if (targetType.equals(Integer.class)) {
                                return castToInt(o);
                        } else if (targetType.equals(Short.class)) {
                                return castToShort(o);
                        } else if (targetType.equals(Byte.class)) {
                                return castToByte(o);
                        } else if (targetType.equals(Character.class)) {
                                return castToChar(o);
                        } else if (targetType.equals(Long.class)) {
                                return castToLong(o);
                        } else if (targetType.equals(Boolean.class)) {
                                return castToBool(o);
                        } else if (targetType.equals(Float.class)) {
                                return castToFloat(o);
                        } else if (targetType.equals(Double.class)) {
                                return castToDouble(o);
                        } else throw new RuntimeException("unknown box type " + targetType);
                }
                if (targetType.equals(int.class)) return castToInt(o);
                if (targetType.equals(short.class)) return castToShort(o);
                if (targetType.equals(byte.class)) return castToByte(o);
                if (targetType.equals(char.class)) return castToChar(o);
                if (targetType.equals(long.class)) return castToLong(o);
                if (targetType.equals(boolean.class)) return castToBool(o);
                if (targetType.equals(float.class)) return castToFloat(o);
                if (targetType.equals(double.class)) return castToDouble(o);
                if (targetType.isArray()) {
                        if (o instanceof java.util.List) {
                                Class<?> component = targetType.getComponentType();
                                java.util.List<?> list = (java.util.List<?>) o;
                                Object arr = Array.newInstance(component, list.size());

                                for (int cursor = 0; cursor < list.size(); ++cursor) {
                                        Object elem = list.get(cursor);
                                        Array.set(arr, cursor, cast(elem, component));
                                }

                                return arr;
                        }
                } else if (Dynamic.isFunctionalAbstractClass(targetType)) {
                        if (o instanceof Function) {
                                Method method = Dynamic.findAbstractMethod(targetType);
                                Method funcMethod = o.getClass().getDeclaredMethods()[0];
                                if (method.getParameterCount() == funcMethod.getParameterCount()) {
                                        int i = 0;
                                        while (true) {
                                                try {
                                                        Class.forName(targetType.getSimpleName() + "$LessTyping$lambda$" + i);
                                                } catch (ClassNotFoundException e) {
                                                        break;
                                                }
                                        }
                                        StringBuilder sb = new StringBuilder();
                                        sb.append("class ").append(targetType.getSimpleName()).append("$LessTyping$lambda$").append(i)
                                                .append("(func:").append(funcMethod.getDeclaringClass().getInterfaces()[0].getName().replace(".", "::"))
                                                .append("):").append(targetType.getName().replace(".", "::")).append("\n")
                                                .append("    ").append(method.getName()).append("(");
                                        boolean isFirst = true;
                                        int index = 0;
                                        for (Class<?> param : method.getParameterTypes()) {
                                                if (isFirst) isFirst = false;
                                                else sb.append(",");
                                                sb.append("p").append(index++).append(":").append(param.getName().replace(".", "::"));
                                        }
                                        sb.append("):").append(method.getReturnType().getName().replace(".", "::")).append("\n")
                                                .append("        ");
                                        if (method.getReturnType() != void.class) sb.append("return ");
                                        sb.append("func.apply(");
                                        isFirst = true;
                                        for (int j = 0; j < index; ++j) {
                                                if (isFirst) isFirst = false;
                                                else sb.append(",");
                                                sb.append("p").append(j);
                                        }
                                        sb.append(")\n");

                                        @SuppressWarnings("unchecked")
                                        Class<?> cls = ((java.util.List<Class<?>>) Utils.eval(sb.toString())).get(0);
                                        Constructor<?> con = cls.getConstructor(funcMethod.getDeclaringClass().getInterfaces()[0]);
                                        return con.newInstance(o);
                                }
                        }
                } else if (Dynamic.isFunctionalInterface(targetType)) {
                        if (o instanceof Function) {
                                Method method = Dynamic.findAbstractMethod(targetType);
                                Method funcMethod = o.getClass().getDeclaredMethods()[0];
                                if (method.getParameterCount() == funcMethod.getParameterCount()) {
                                        return Proxy.newProxyInstance(o.getClass().getClassLoader(), new Class[]{method.getDeclaringClass()},
                                                (proxy, method1, args) -> {
                                                        if (method1.equals(method)) {
                                                                funcMethod.setAccessible(true);
                                                                Object res = funcMethod.invoke(o, args);
                                                                if (method1.getReturnType() == void.class) {
                                                                        return null;
                                                                } else {
                                                                        return res;
                                                                }
                                                        } else {
                                                                return method1.invoke(proxy, args);
                                                        }
                                                });
                                }
                        }
                } else throw new LtBug("unsupported type cast");
                throw new ClassCastException("cannot cast " + o + " (" + o.getClass().getName() + ") to " + targetType.getName());
        }

        public static int castToInt(Object o) {
                if (o instanceof Number) return ((Number) o).intValue();
                throw new ClassCastException("cannot cast " + o + " (" + o.getClass().getName() + ") to int");
        }

        public static long castToLong(Object o) {
                if (o instanceof Number) return ((Number) o).longValue();
                throw new ClassCastException("cannot cast " + o + " (" + o.getClass().getName() + ") to long");
        }

        public static short castToShort(Object o) {
                if (o instanceof Number) return ((Number) o).shortValue();
                throw new ClassCastException("cannot cast " + o + " (" + o.getClass().getName() + ") to short");
        }

        public static byte castToByte(Object o) {
                if (o instanceof Number) return ((Number) o).byteValue();
                throw new ClassCastException("cannot cast " + o + " (" + o.getClass().getName() + ") to byte");
        }

        public static float castToFloat(Object o) {
                if (o instanceof Number) return ((Number) o).floatValue();
                throw new ClassCastException("cannot cast " + o + " (" + o.getClass().getName() + ") to float");
        }

        public static double castToDouble(Object o) {
                if (o instanceof Number) return ((Number) o).doubleValue();
                throw new ClassCastException("cannot cast " + o + " (" + o.getClass().getName() + ") to double");
        }

        public static boolean castToBool(Object o) {
                // check null and undefined
                if (o == null || o instanceof Undefined) return false;
                // check Boolean object
                if (o instanceof Boolean) return (Boolean) o;
                // check number not 0
                if (o instanceof Number) return ((Number) o).doubleValue() != 0;
                // otherwise return true
                return true;
        }

        public static char castToChar(Object o) {
                if (o instanceof Number) return (char) ((Number) o).intValue();
                if (o instanceof Character) return (Character) o;
                if (o instanceof CharSequence && ((CharSequence) o).length() == 1) return ((CharSequence) o).charAt(0);
                throw new RuntimeException("cannot cast " + o + " to char");
        }

        /**
         * get field value.<br>
         * if field not found , then the method would try to invoke get(fieldName)<br>
         * or the method would return <tt>undefined</tt>
         *
         * @param o           object
         * @param fieldName   field name
         * @param callerClass caller class
         * @return the value or undefined
         */
        @SuppressWarnings("unused")
        public static Object getField(Object o, String fieldName, Class<?> callerClass) {
                if (o.getClass().isArray()) {
                        if (fieldName.equals("length")) {
                                return Array.getLength(o);
                        } else return Undefined.get();
                }

                // try to get field
                try {
                        Field f = o.getClass().getDeclaredField(fieldName);
                        f.setAccessible(true);
                        return f.get(o);
                } catch (Throwable ignore) {
                }
                // try to find `get(fieldName)`
                try {
                        return Dynamic.invoke(o.getClass(), o, callerClass, "get", new boolean[]{false}, new Object[]{fieldName});
                } catch (Throwable ignore) {
                }
                // try _number
                if (fieldName.startsWith("_")) {
                        try {
                                Integer i = Integer.parseInt(fieldName.substring(1));
                                try {
                                        return Dynamic.invoke(o.getClass(), o, callerClass, "get", new boolean[]{false}, new Object[]{i});
                                } catch (Throwable ignore) {
                                }
                        } catch (NumberFormatException ignore) {
                        }
                }
                // try to find `fieldName()`
                try {
                        return Dynamic.invoke(o.getClass(), o, callerClass, fieldName, new boolean[0], new Object[0]);
                } catch (Throwable ignore) {
                }
                return Undefined.get();
        }

        /**
         * put field.<br>
         * if field not found , then the method would try to invoke set(fieldName, value)<br>
         * the method calls {@link Dynamic#invoke(Class, Object, Class, String, boolean[], Object[])}, and <code>set(fieldName,value)</code> may be changed to <code>put(fieldName, value)</code>
         *
         * @param o           object
         * @param fieldName   field name
         * @param value       the value to set
         * @param callerClass caller class
         * @throws Throwable exceptions
         */
        public static void putField(Object o, String fieldName, Object value, Class<?> callerClass) throws Throwable {
                // try to put field
                try {
                        Field f = o.getClass().getDeclaredField(fieldName);
                        f.setAccessible(true);
                        f.set(o, cast(value, f.getType()));
                        return;
                } catch (Throwable ignore) {
                }
                // try to find `set(fieldName,value)`
                // invoke dynamic would try to find set then try to find put
                Dynamic.invoke(o.getClass(), o, callerClass, "set", new boolean[]{false, false}, new Object[]{fieldName, value});
        }

        public static final int COMPARE_MODE_GT = 0b001;
        public static final int COMPARE_MODE_EQ = 0b010;
        public static final int COMPARE_MODE_LT = 0b100;

        /**
         * change compare result into boolean
         *
         * @param result compare result.
         * @param mode   {@link #COMPARE_MODE_EQ} {@link #COMPARE_MODE_GT} {@link #COMPARE_MODE_LT}, can be linked with + or | operator
         * @return boolean value
         */
        public static boolean compare(int result, int mode) {
                // mode is EQ
                if ((mode & COMPARE_MODE_EQ) == COMPARE_MODE_EQ && result == 0) return true;
                // mode is GT
                if ((mode & COMPARE_MODE_GT) == COMPARE_MODE_GT && result > 0) return true;
                // mode is LT
                if ((mode & COMPARE_MODE_LT) == COMPARE_MODE_LT && result < 0) return true;
                // otherwise
                return false;
        }

        /**
         * compare two refs
         *
         * @param a a
         * @param b b
         * @return true or false
         */
        public static boolean compareRef(Object a, Object b) {
                if (a == b) return true;
                //
                return false;
        }

        /**
         * <code>is</code> operator<br>
         *
         * @param a           a
         * @param b           b
         * @param callerClass caller class
         * @return true/false
         */
        public static boolean is(Object a, Object b, Class<?> callerClass) {
                if (a == null && b == null) return true;
                // not both a and b are null
                if (a == null || b == null) return false;
                // a and b are not null
                if (a == b || a.equals(b)) return true;
                // a!=b and a.equals(b) is false
                if (b instanceof Class) if (((Class) b).isInstance(a)) return true;
                // b is not class or (b is class and a not instanceof b)
                try {
                        return castToBool(Dynamic.invoke(a.getClass(), a, callerClass, "is", new boolean[]{false}, new Object[]{b}));
                } catch (Throwable ignore) {
                }
                return false;
        }

        /**
         * <code>not</code> operator
         *
         * @param a           a
         * @param b           b
         * @param callerClass caller class
         * @return true/false
         */
        public static boolean not(Object a, Object b, Class<?> callerClass) {
                if (a == null && b == null) return false;
                // not both a and b are null
                if (a == null || b == null) return true;
                // a and b are not null
                if (a == b || a.equals(b)) return false;
                // a!=b and a.equals(b) is false
                if (b instanceof Class) if (((Class) b).isInstance(a)) return false;
                // b is not class or (b is class and a not instanceof b)
                try {
                        return castToBool(Dynamic.invoke(a.getClass(), a, callerClass, "not", new boolean[]{false}, new Object[]{b}));
                } catch (Throwable ignore) {
                }
                return true;
        }

        /**
         * get wrapped object in {@link Wrapper#object} or simply return the throwable object
         *
         * @param t the wrapper object or other throwable
         * @return the retrieved object
         */
        public static Object throwableWrapperObject(Throwable t) {
                if (t instanceof Wrapper) return ((Wrapper) t).object;
                return t;
        }

        /**
         * get hash code of a object
         *
         * @param o the object
         * @return hash code of the object (or 0 if it's null)
         */
        public static int getHashCode(Object o) {
                if (o == null) return 0;
                return o.hashCode();
        }
}
