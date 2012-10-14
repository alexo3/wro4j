/**
 * Copyright Alex Objelean
 */
package ro.isdc.wro.model.group.processor;

import static junit.framework.Assert.assertNotNull;

import java.util.concurrent.Callable;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import ro.isdc.wro.WroRuntimeException;
import ro.isdc.wro.config.Context;
import ro.isdc.wro.config.ReadOnlyContext;
import ro.isdc.wro.config.jmx.WroConfiguration;
import ro.isdc.wro.manager.callback.LifecycleCallbackRegistry;
import ro.isdc.wro.manager.factory.BaseWroManagerFactory;
import ro.isdc.wro.model.group.Inject;
import ro.isdc.wro.model.resource.processor.ResourcePreProcessor;
import ro.isdc.wro.model.resource.processor.decorator.CopyrightKeeperProcessorDecorator;
import ro.isdc.wro.model.resource.processor.impl.js.JSMinProcessor;


/**
 * @author Alex Objelean
 * @created 12 Dec 2011
 */
public class TestInjector {
  private Injector victim;

  @Before
  public void setUp() {
    Context.set(Context.standaloneContext());
    initializeValidInjector();
  }

  @Test(expected = NullPointerException.class)
  public void cannotAcceptNullMap() {
    victim = new Injector(null);
  }

  @Test
  public void shouldAcceptInjectInitializedManager() {
    initializeValidInjector();
  }

  private void initializeValidInjector() {
    victim = InjectorBuilder.create(new BaseWroManagerFactory()).build();
  }

  @Test(expected = WroRuntimeException.class)
  public void cannotInjectUnsupportedAndUnitializedType() {
    initializeValidInjector();
    final Object inner = new Object() {
      @Inject
      private Object object;
    };
    victim.inject(inner);
  }

  @Test
  public void shouldInjectSupportedType()
      throws Exception {
    initializeValidInjector();
    final Callable<?> inner = new Callable<Void>() {
      @Inject
      private LifecycleCallbackRegistry object;

      public Void call()
          throws Exception {
        Assert.assertNotNull(object);
        return null;
      }
    };
    victim.inject(inner);
    inner.call();
  }

  @Test
  public void shouldInjectContext()
      throws Exception {
    // Cannot reuse this part, because generic type is not inferred correctly at runtime
    final Callable<?> inner = new Callable<Void>() {
      @Inject
      private ReadOnlyContext object;

      public Void call()
          throws Exception {
        Assert.assertNotNull(object);
        return null;
      }
    };
    victim.inject(inner);
    inner.call();
  }

  @Test(expected = WroRuntimeException.class)
  public void canInjectContextOutsideOfContextScope()
      throws Exception {
    // remove the context explicitly
    Context.unset();
    shouldInjectContext();
  }

  @Test
  public void shouldInjectWroConfiguration()
      throws Exception {
    final Callable<?> inner = new Callable<Void>() {
      @Inject
      private WroConfiguration object;

      public Void call()
          throws Exception {
        Assert.assertNotNull(object);
        return null;
      }
    };
    victim.inject(inner);
    inner.call();
  }

  private class TestProcessor
      extends JSMinProcessor {
    @Inject
    private ReadOnlyContext context;
  }

  @Test
  public void shouldInjectDecoratedProcessor() {
    final TestProcessor testProcessor = new TestProcessor();
    final ResourcePreProcessor processor = CopyrightKeeperProcessorDecorator.decorate(testProcessor);

    final Injector injector = InjectorBuilder.create(new BaseWroManagerFactory()).build();
    injector.inject(processor);
    assertNotNull(testProcessor.context);
  }

  @Test
  public void shouldInjectUnsupportedButInitializedTypes() {
    final InnerTest inner = new InnerTest();
    victim.inject(inner);
    Assert.assertEquals(new InnerTest().unsupportedInitializedType, inner.unsupportedInitializedType);
  }

  private static class InnerTest {
    @Inject
    private final String unsupportedInitializedType = "";
  }

  @After
  public void tearDown() {
    Context.unset();
  }
}
