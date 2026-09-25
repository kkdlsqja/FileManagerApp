package com.filemanager.app.network;

public class PcDevice {
    // 서버의 엔티티(Entity) 변수명과 반드시 똑같아야 합니다.
    private Long id;
    private String pcName;
    private String pcIdentifier;

    // 데이터를 읽어오기 위한 Getter 메서드들
    public Long getId() {
        return id;
    }

    public String getPcName() {
        return pcName;
    }

    public String getPcIdentifier() {
        return pcIdentifier;
    }
}
