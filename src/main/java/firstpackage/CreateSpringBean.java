package firstpackage;

import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.io.ClassPathResource;

/**
 * @Organization: FinTeach-Dev
 * @Author: JeRome
 * @Date: 2018-12-16
 * @Description: my first spring bean
 */
public class CreateSpringBean {
    public static void main(String[] args) {
        // 构造工厂
        DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
        // 新增Xml阅读器
        XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(factory);
        // 规则注册入容
        reader.loadBeanDefinitions(new ClassPathResource("ClassPath:spring-config.xml"));
        UserDto userDto = factory.getBean(UserDto.class);

    }
}
