package firstpackage;

/**
 * @Organization: FinTeach-Dev
 * @Author: JeRome
 * @Date: 2018-12-16
 * @Description: user dto
 */
public class UserDto {

    private String name;
    private int age;
    private int dptNo;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public int getDptNo() {
        return dptNo;
    }

    public void setDptNo(int dptNo) {
        this.dptNo = dptNo;
    }
}
