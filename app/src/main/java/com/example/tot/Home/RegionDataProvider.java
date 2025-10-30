package com.example.tot.Home;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 한국 시/도 및 시군구 데이터 제공 클래스
 * 서버 연동 시 이 클래스를 통해 지역 코드로 필터링 가능
 */
public class RegionDataProvider {

    /**
     * 지역 정보를 담는 클래스
     */
    public static class Region {
        private String code;        // 서버 연동용 지역 코드
        private String name;        // 표시할 지역명
        private int population;     // 인구 (시/도 정렬용)

        public Region(String code, String name, int population) {
            this.code = code;
            this.name = name;
            this.population = population;
        }

        public String getCode() {
            return code;
        }

        public String getName() {
            return name;
        }

        public int getPopulation() {
            return population;
        }
    }

    /**
     * 전체 시/도 데이터 반환 (인구 순 정렬)
     * 인구 데이터는 2024년 기준 추정치
     */
    public static List<Region> getProvinces() {
        List<Region> provinces = new ArrayList<>();

        // 인구가 많은 순서대로 정렬
        provinces.add(new Region("11", "서울", 9500000));
        provinces.add(new Region("41", "경기", 13500000));
        provinces.add(new Region("26", "부산", 3300000));
        provinces.add(new Region("28", "인천", 3000000));
        provinces.add(new Region("27", "대구", 2400000));
        provinces.add(new Region("30", "대전", 1450000));
        provinces.add(new Region("29", "광주", 1440000));
        provinces.add(new Region("31", "울산", 1120000));
        provinces.add(new Region("50", "세종", 380000));
        provinces.add(new Region("48", "경남", 3300000));
        provinces.add(new Region("47", "경북", 2600000));
        provinces.add(new Region("45", "전남", 1800000));
        provinces.add(new Region("46", "전북", 1780000));
        provinces.add(new Region("44", "충남", 2100000));
        provinces.add(new Region("43", "충북", 1600000));
        provinces.add(new Region("42", "강원", 1540000));
        provinces.add(new Region("49", "제주", 680000));

        return provinces;
    }

    /**
     * 특정 시/도의 시군구 데이터 반환 (가나다 순 정렬)
     */
    public static List<Region> getCities(String provinceCode) {
        Map<String, List<Region>> cityData = getAllCityData();
        List<Region> cities = cityData.get(provinceCode);

        if (cities != null) {
            // 가나다 순 정렬
            Collections.sort(cities, (a, b) -> a.getName().compareTo(b.getName()));
        }

        return cities != null ? cities : new ArrayList<>();
    }

    /**
     * 모든 시군구 데이터 (코드 포함)
     */
    private static Map<String, List<Region>> getAllCityData() {
        Map<String, List<Region>> cityData = new LinkedHashMap<>();

        // 서울특별시 (11)
        List<Region> seoul = new ArrayList<>();
        seoul.add(new Region("11680", "강남구", 0));
        seoul.add(new Region("11740", "강동구", 0));
        seoul.add(new Region("11305", "강북구", 0));
        seoul.add(new Region("11500", "강서구", 0));
        seoul.add(new Region("11620", "관악구", 0));
        seoul.add(new Region("11215", "광진구", 0));
        seoul.add(new Region("11530", "구로구", 0));
        seoul.add(new Region("11545", "금천구", 0));
        seoul.add(new Region("11350", "노원구", 0));
        seoul.add(new Region("11320", "도봉구", 0));
        seoul.add(new Region("11230", "동대문구", 0));
        seoul.add(new Region("11590", "동작구", 0));
        seoul.add(new Region("11440", "마포구", 0));
        seoul.add(new Region("11410", "서대문구", 0));
        seoul.add(new Region("11650", "서초구", 0));
        seoul.add(new Region("11200", "성동구", 0));
        seoul.add(new Region("11290", "성북구", 0));
        seoul.add(new Region("11710", "송파구", 0));
        seoul.add(new Region("11470", "양천구", 0));
        seoul.add(new Region("11560", "영등포구", 0));
        seoul.add(new Region("11170", "용산구", 0));
        seoul.add(new Region("11380", "은평구", 0));
        seoul.add(new Region("11110", "종로구", 0));
        seoul.add(new Region("11140", "중구", 0));
        seoul.add(new Region("11260", "중랑구", 0));
        cityData.put("11", seoul);

        // 부산광역시 (26)
        List<Region> busan = new ArrayList<>();
        busan.add(new Region("26440", "강서구", 0));
        busan.add(new Region("26410", "금정구", 0));
        busan.add(new Region("26710", "기장군", 0));
        busan.add(new Region("26290", "남구", 0));
        busan.add(new Region("26170", "동구", 0));
        busan.add(new Region("26260", "동래구", 0));
        busan.add(new Region("26230", "부산진구", 0));
        busan.add(new Region("26320", "북구", 0));
        busan.add(new Region("26530", "사상구", 0));
        busan.add(new Region("26380", "사하구", 0));
        busan.add(new Region("26140", "서구", 0));
        busan.add(new Region("26500", "수영구", 0));
        busan.add(new Region("26470", "연제구", 0));
        busan.add(new Region("26200", "영도구", 0));
        busan.add(new Region("26110", "중구", 0));
        busan.add(new Region("26350", "해운대구", 0));
        cityData.put("26", busan);

        // 대구광역시 (27)
        List<Region> daegu = new ArrayList<>();
        daegu.add(new Region("27200", "남구", 0));
        daegu.add(new Region("27290", "달서구", 0));
        daegu.add(new Region("27710", "달성군", 0));
        daegu.add(new Region("27140", "동구", 0));
        daegu.add(new Region("27230", "북구", 0));
        daegu.add(new Region("27170", "서구", 0));
        daegu.add(new Region("27260", "수성구", 0));
        daegu.add(new Region("27110", "중구", 0));
        cityData.put("27", daegu);

        // 인천광역시 (28)
        List<Region> incheon = new ArrayList<>();
        incheon.add(new Region("28710", "강화군", 0));
        incheon.add(new Region("28245", "계양구", 0));
        incheon.add(new Region("28200", "남동구", 0));
        incheon.add(new Region("28140", "동구", 0));
        incheon.add(new Region("28177", "미추홀구", 0));
        incheon.add(new Region("28237", "부평구", 0));
        incheon.add(new Region("28260", "서구", 0));
        incheon.add(new Region("28185", "연수구", 0));
        incheon.add(new Region("28720", "옹진군", 0));
        incheon.add(new Region("28110", "중구", 0));
        cityData.put("28", incheon);

        // 광주광역시 (29)
        List<Region> gwangju = new ArrayList<>();
        gwangju.add(new Region("29200", "광산구", 0));
        gwangju.add(new Region("29155", "남구", 0));
        gwangju.add(new Region("29110", "동구", 0));
        gwangju.add(new Region("29170", "북구", 0));
        gwangju.add(new Region("29140", "서구", 0));
        cityData.put("29", gwangju);

        // 대전광역시 (30)
        List<Region> daejeon = new ArrayList<>();
        daejeon.add(new Region("30230", "대덕구", 0));
        daejeon.add(new Region("30110", "동구", 0));
        daejeon.add(new Region("30170", "서구", 0));
        daejeon.add(new Region("30200", "유성구", 0));
        daejeon.add(new Region("30140", "중구", 0));
        cityData.put("30", daejeon);

        // 울산광역시 (31)
        List<Region> ulsan = new ArrayList<>();
        ulsan.add(new Region("31140", "남구", 0));
        ulsan.add(new Region("31170", "동구", 0));
        ulsan.add(new Region("31200", "북구", 0));
        ulsan.add(new Region("31710", "울주군", 0));
        ulsan.add(new Region("31110", "중구", 0));
        cityData.put("31", ulsan);

        // 세종특별자치시 (36)
        List<Region> sejong = new ArrayList<>();
        sejong.add(new Region("36110", "세종시", 0));
        cityData.put("50", sejong);

        // 경기도 (41)
        List<Region> gyeonggi = new ArrayList<>();
        gyeonggi.add(new Region("41820", "가평군", 0));
        gyeonggi.add(new Region("41281", "고양시", 0));
        gyeonggi.add(new Region("41290", "과천시", 0));
        gyeonggi.add(new Region("41210", "광명시", 0));
        gyeonggi.add(new Region("41610", "광주시", 0));
        gyeonggi.add(new Region("41410", "구리시", 0));
        gyeonggi.add(new Region("41310", "군포시", 0));
        gyeonggi.add(new Region("41570", "김포시", 0));
        gyeonggi.add(new Region("41360", "남양주시", 0));
        gyeonggi.add(new Region("41250", "동두천시", 0));
        gyeonggi.add(new Region("41190", "부천시", 0));
        gyeonggi.add(new Region("41131", "성남시", 0));
        gyeonggi.add(new Region("41110", "수원시", 0));
        gyeonggi.add(new Region("41390", "시흥시", 0));
        gyeonggi.add(new Region("41550", "안성시", 0));
        gyeonggi.add(new Region("41270", "안양시", 0));
        gyeonggi.add(new Region("41630", "양평군", 0));
        gyeonggi.add(new Region("41830", "양주시", 0));
        gyeonggi.add(new Region("41670", "여주시", 0));
        gyeonggi.add(new Region("41800", "연천군", 0));
        gyeonggi.add(new Region("41370", "오산시", 0));
        gyeonggi.add(new Region("41460", "용인시", 0));
        gyeonggi.add(new Region("41430", "의왕시", 0));
        gyeonggi.add(new Region("41150", "의정부시", 0));
        gyeonggi.add(new Region("41500", "이천시", 0));
        gyeonggi.add(new Region("41480", "파주시", 0));
        gyeonggi.add(new Region("41220", "평택시", 0));
        gyeonggi.add(new Region("41650", "포천시", 0));
        gyeonggi.add(new Region("41450", "하남시", 0));
        gyeonggi.add(new Region("41590", "화성시", 0));
        cityData.put("41", gyeonggi);

        // 강원특별자치도 (42)
        List<Region> gangwon = new ArrayList<>();
        gangwon.add(new Region("42150", "강릉시", 0));
        gangwon.add(new Region("42820", "고성군", 0));
        gangwon.add(new Region("42170", "동해시", 0));
        gangwon.add(new Region("42230", "삼척시", 0));
        gangwon.add(new Region("42210", "속초시", 0));
        gangwon.add(new Region("42830", "양구군", 0));
        gangwon.add(new Region("42750", "양양군", 0));
        gangwon.add(new Region("42720", "영월군", 0));
        gangwon.add(new Region("42790", "인제군", 0));
        gangwon.add(new Region("42730", "정선군", 0));
        gangwon.add(new Region("42780", "철원군", 0));
        gangwon.add(new Region("42110", "춘천시", 0));
        gangwon.add(new Region("42190", "태백시", 0));
        gangwon.add(new Region("42740", "평창군", 0));
        gangwon.add(new Region("42770", "홍천군", 0));
        gangwon.add(new Region("42800", "화천군", 0));
        gangwon.add(new Region("42760", "횡성군", 0));
        gangwon.add(new Region("42130", "원주시", 0));
        cityData.put("42", gangwon);

        // 충청북도 (43)
        List<Region> chungbuk = new ArrayList<>();
        chungbuk.add(new Region("43730", "괴산군", 0));
        chungbuk.add(new Region("43800", "단양군", 0));
        chungbuk.add(new Region("43720", "보은군", 0));
        chungbuk.add(new Region("43740", "영동군", 0));
        chungbuk.add(new Region("43745", "옥천군", 0));
        chungbuk.add(new Region("43770", "음성군", 0));
        chungbuk.add(new Region("43150", "제천시", 0));
        chungbuk.add(new Region("43750", "증평군", 0));
        chungbuk.add(new Region("43760", "진천군", 0));
        chungbuk.add(new Region("43111", "청주시", 0));
        chungbuk.add(new Region("43130", "충주시", 0));
        cityData.put("43", chungbuk);

        // 충청남도 (44)
        List<Region> chungnam = new ArrayList<>();
        chungnam.add(new Region("44200", "공주시", 0));
        chungnam.add(new Region("44710", "금산군", 0));
        chungnam.add(new Region("44230", "논산시", 0));
        chungnam.add(new Region("44270", "당진시", 0));
        chungnam.add(new Region("44180", "보령시", 0));
        chungnam.add(new Region("44760", "부여군", 0));
        chungnam.add(new Region("44210", "서산시", 0));
        chungnam.add(new Region("44770", "서천군", 0));
        chungnam.add(new Region("44250", "계룡시", 0));
        chungnam.add(new Region("44790", "예산군", 0));
        chungnam.add(new Region("44825", "태안군", 0));
        chungnam.add(new Region("44131", "천안시", 0));
        chungnam.add(new Region("44800", "청양군", 0));
        chungnam.add(new Region("44810", "홍성군", 0));
        chungnam.add(new Region("44150", "아산시", 0));
        cityData.put("44", chungnam);

        // 전북특별자치도 (45) -> 전라북도
        List<Region> jeonbuk = new ArrayList<>();
        jeonbuk.add(new Region("45210", "고창군", 0));
        jeonbuk.add(new Region("45140", "군산시", 0));
        jeonbuk.add(new Region("45180", "김제시", 0));
        jeonbuk.add(new Region("45190", "남원시", 0));
        jeonbuk.add(new Region("45750", "무주군", 0));
        jeonbuk.add(new Region("45220", "부안군", 0));
        jeonbuk.add(new Region("45800", "순창군", 0));
        jeonbuk.add(new Region("45790", "임실군", 0));
        jeonbuk.add(new Region("45210", "장수군", 0));
        jeonbuk.add(new Region("45130", "정읍시", 0));
        jeonbuk.add(new Region("45110", "전주시", 0));
        jeonbuk.add(new Region("45710", "진안군", 0));
        jeonbuk.add(new Region("45730", "완주군", 0));
        jeonbuk.add(new Region("45170", "익산시", 0));
        cityData.put("46", jeonbuk);

        // 전라남도 (46) -> 전남
        List<Region> jeonnam = new ArrayList<>();
        jeonnam.add(new Region("46770", "강진군", 0));
        jeonnam.add(new Region("46710", "고흥군", 0));
        jeonnam.add(new Region("46810", "곡성군", 0));
        jeonnam.add(new Region("46820", "구례군", 0));
        jeonnam.add(new Region("46910", "담양군", 0));
        jeonnam.add(new Region("46130", "목포시", 0));
        jeonnam.add(new Region("46840", "무안군", 0));
        jeonnam.add(new Region("46780", "보성군", 0));
        jeonnam.add(new Region("46150", "순천시", 0));
        jeonnam.add(new Region("46900", "신안군", 0));
        jeonnam.add(new Region("46110", "여수시", 0));
        jeonnam.add(new Region("46830", "영광군", 0));
        jeonnam.add(new Region("46890", "영암군", 0));
        jeonnam.add(new Region("46880", "완도군", 0));
        jeonnam.add(new Region("46870", "장성군", 0));
        jeonnam.add(new Region("46800", "장흥군", 0));
        jeonnam.add(new Region("46910", "진도군", 0));
        jeonnam.add(new Region("46860", "함평군", 0));
        jeonnam.add(new Region("46790", "해남군", 0));
        jeonnam.add(new Region("46770", "화순군", 0));
        jeonnam.add(new Region("46170", "나주시", 0));
        jeonnam.add(new Region("46230", "광양시", 0));
        cityData.put("45", jeonnam);

        // 경상북도 (47)
        List<Region> gyeongbuk = new ArrayList<>();
        gyeongbuk.add(new Region("47290", "경산시", 0));
        gyeongbuk.add(new Region("47130", "경주시", 0));
        gyeongbuk.add(new Region("47830", "고령군", 0));
        gyeongbuk.add(new Region("47190", "구미시", 0));
        gyeongbuk.add(new Region("47720", "군위군", 0));
        gyeongbuk.add(new Region("47150", "김천시", 0));
        gyeongbuk.add(new Region("47280", "문경시", 0));
        gyeongbuk.add(new Region("47920", "봉화군", 0));
        gyeongbuk.add(new Region("47250", "상주시", 0));
        gyeongbuk.add(new Region("47840", "성주군", 0));
        gyeongbuk.add(new Region("47170", "안동시", 0));
        gyeongbuk.add(new Region("47770", "영덕군", 0));
        gyeongbuk.add(new Region("47230", "영주시", 0));
        gyeongbuk.add(new Region("47760", "영양군", 0));
        gyeongbuk.add(new Region("47210", "영천시", 0));
        gyeongbuk.add(new Region("47900", "예천군", 0));
        gyeongbuk.add(new Region("47940", "울릉군", 0));
        gyeongbuk.add(new Region("47930", "울진군", 0));
        gyeongbuk.add(new Region("47730", "의성군", 0));
        gyeongbuk.add(new Region("47820", "청도군", 0));
        gyeongbuk.add(new Region("47750", "청송군", 0));
        gyeongbuk.add(new Region("47850", "칠곡군", 0));
        gyeongbuk.add(new Region("47111", "포항시", 0));
        cityData.put("47", gyeongbuk);

        // 경상남도 (48)
        List<Region> gyeongnam = new ArrayList<>();
        gyeongnam.add(new Region("48310", "거제시", 0));
        gyeongnam.add(new Region("48880", "거창군", 0));
        gyeongnam.add(new Region("48820", "고성군", 0));
        gyeongnam.add(new Region("48840", "남해군", 0));
        gyeongnam.add(new Region("48125", "김해시", 0));
        gyeongnam.add(new Region("48170", "밀양시", 0));
        gyeongnam.add(new Region("48720", "산청군", 0));
        gyeongnam.add(new Region("48240", "사천시", 0));
        gyeongnam.add(new Region("48860", "합천군", 0));
        gyeongnam.add(new Region("48730", "함양군", 0));
        gyeongnam.add(new Region("48850", "하동군", 0));
        gyeongnam.add(new Region("48270", "양산시", 0));
        gyeongnam.add(new Region("48720", "의령군", 0));
        gyeongnam.add(new Region("48121", "창원시", 0));
        gyeongnam.add(new Region("48770", "창녕군", 0));
        gyeongnam.add(new Region("48220", "통영시", 0));
        gyeongnam.add(new Region("48890", "함안군", 0));
        gyeongnam.add(new Region("48139", "진주시", 0));
        cityData.put("48", gyeongnam);

        // 제주특별자치도 (49) -> 제주
        List<Region> jeju = new ArrayList<>();
        jeju.add(new Region("50110", "제주시", 0));
        jeju.add(new Region("50130", "서귀포시", 0));
        cityData.put("49", jeju);

        return cityData;
    }

    /**
     * 지역 코드로 지역명 찾기 (서버 응답 파싱용)
     */
    public static String getRegionNameByCode(String code) {
        // 시/도 검색
        for (Region province : getProvinces()) {
            if (province.getCode().equals(code)) {
                return province.getName();
            }
        }

        // 시군구 검색
        Map<String, List<Region>> allCities = getAllCityData();
        for (List<Region> cities : allCities.values()) {
            for (Region city : cities) {
                if (city.getCode().equals(code)) {
                    return city.getName();
                }
            }
        }

        return null;
    }
}