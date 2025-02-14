package com.example.admin.sinlineapplication.otherobj;

public class Exception {
    private String Emessage;
    private String Etype;
    private String Etime;
    private String Eplace;
    private String Ephone;

    public Exception(String Emessage, String Etype, String Etime, String Eplace, String Ephone) {
        this.Emessage = Emessage;
        this.Etype = Etype;
        this.Etime = Etime;
        this.Eplace = Eplace;
        this.Ephone = Ephone;
        System.out.println("异常信息：" + Emessage + "异常类型：" + Etype + "异常时间：" + Etime + "异常地点：" + Eplace + "异常手机号：" + Ephone);
    }











    public String getEmessage() {
        return Emessage;
    }
    public void setEmessage(String emessage) {
        Emessage = emessage;
    }
    public String getEtype() {
        return Etype;
    }
    public void setEtype(String etype) {
        Etype = etype;
    }
    public String getEtime() {
        return Etime;
    }

}
