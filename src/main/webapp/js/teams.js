/*
    Basketball League Manager — Takımlar Sayfası Mantığı
    ADR-0001: Statik JS, /api/teams REST uç noktasını çağırır;
    ulaşamazsa yerleşik EuroLeague demo verisine düşer.
*/
(function () {
    'use strict';

    /* ------------------------------------------------------------------ */
    /* Demo Verisi — EuroLeague 2024-2025 Referans Kadroları               */
    /* ------------------------------------------------------------------ */
    var DEMO_TEAMS = [
        {
            id: 1,
            name: 'Anadolu Efes',
            city: 'İstanbul',
            country: 'Türkiye',
            arena: 'Sinan Erdem Spor Salonu',
            capacity: 22000,
            coach: 'Tomás Olsson',
            founded: 1976,
            code: 'EFS',
            players: [
                { id: 101, number: 1,  name: 'Rodrigue Beaubois',  pos: 'PG', age: 36, height: '191 cm', weight: '87 kg', nationality: 'Fransız',   status: 'available', captain: false },
                { id: 102, number: 7,  name: 'Shane Larkin',       pos: 'PG', age: 32, height: '180 cm', weight: '79 kg', nationality: 'Amerikalı',  status: 'available', captain: true  },
                { id: 103, number: 10, name: 'Vasilije Micic',     pos: 'SG', age: 30, height: '196 cm', weight: '90 kg', nationality: 'Sırp',       status: 'available', captain: false },
                { id: 104, number: 5,  name: 'Dogus Balbay',       pos: 'SG', age: 34, height: '185 cm', weight: '84 kg', nationality: 'Türk',       status: 'injured',   captain: false },
                { id: 105, number: 24, name: 'James Anderson',     pos: 'SF', age: 36, height: '196 cm', weight: '99 kg', nationality: 'Amerikalı',  status: 'available', captain: false },
                { id: 106, number: 11, name: 'Alec Peters',        pos: 'SF', age: 30, height: '203 cm', weight: '100 kg', nationality: 'Amerikalı', status: 'doubtful',  captain: false },
                { id: 107, number: 22, name: 'Adrien Moerman',     pos: 'PF', age: 33, height: '203 cm', weight: '104 kg', nationality: 'Fransız',   status: 'available', captain: false },
                { id: 108, number: 3,  name: 'Sertaç Şanlı',      pos: 'PF', age: 26, height: '205 cm', weight: '107 kg', nationality: 'Türk',      status: 'available', captain: false },
                { id: 109, number: 55, name: 'Bryant Dunston',     pos: 'C',  age: 36, height: '211 cm', weight: '109 kg', nationality: 'Amerikalı', status: 'available', captain: false },
                { id: 110, number: 92, name: 'Ömer Faruk Yurtseven', pos: 'C', age: 27, height: '216 cm', weight: '118 kg', nationality: 'Türk',    status: 'available', captain: false },
                { id: 111, number: 18, name: 'Burak Can Yıldızlı', pos: 'PG', age: 23, height: '192 cm', weight: '82 kg', nationality: 'Türk',      status: 'available', captain: false },
                { id: 112, number: 33, name: 'Marius Grigonis',    pos: 'SF', age: 30, height: '200 cm', weight: '97 kg', nationality: 'Litvanyalı', status: 'available', captain: false }
            ]
        },
        {
            id: 2,
            name: 'Real Madrid',
            city: 'Madrid',
            country: 'İspanya',
            arena: 'WiZink Center',
            capacity: 17500,
            coach: 'Chus Mateo',
            founded: 1931,
            code: 'RMA',
            players: [
                { id: 201, number: 11, name: 'Facundo Campazzo',   pos: 'PG', age: 34, height: '179 cm', weight: '79 kg', nationality: 'Arjantinli', status: 'available', captain: false },
                { id: 202, number: 7,  name: 'Fabien Causeur',     pos: 'SG', age: 37, height: '196 cm', weight: '90 kg', nationality: 'Fransız',    status: 'available', captain: false },
                { id: 203, number: 3,  name: 'Sergio Llull',       pos: 'SG', age: 37, height: '190 cm', weight: '86 kg', nationality: 'İspanyol',   status: 'available', captain: true  },
                { id: 204, number: 23, name: 'Mario Hezonja',      pos: 'SF', age: 30, height: '203 cm', weight: '100 kg', nationality: 'Hırvat',    status: 'available', captain: false },
                { id: 205, number: 10, name: 'Luka Doncic',        pos: 'SF', age: 25, height: '201 cm', weight: '104 kg', nationality: 'Sloven',    status: 'available', captain: false },
                { id: 206, number: 12, name: 'Gabriel Deck',       pos: 'PF', age: 30, height: '203 cm', weight: '102 kg', nationality: 'Arjantinli', status: 'doubtful', captain: false },
                { id: 207, number: 22, name: 'Guerschon Yabusele', pos: 'PF', age: 29, height: '203 cm', weight: '107 kg', nationality: 'Fransız',   status: 'available', captain: false },
                { id: 208, number: 5,  name: 'Rudy Fernández',     pos: 'SF', age: 39, height: '196 cm', weight: '90 kg', nationality: 'İspanyol',   status: 'injured',   captain: false },
                { id: 209, number: 41, name: 'Edy Tavares',        pos: 'C',  age: 32, height: '221 cm', weight: '117 kg', nationality: 'Cabo Verdeli', status: 'available', captain: false },
                { id: 210, number: 13, name: 'Walter Tavares',     pos: 'C',  age: 32, height: '221 cm', weight: '114 kg', nationality: 'Cabo Verdeli', status: 'available', captain: false },
                { id: 211, number: 25, name: 'Dzanan Musa',        pos: 'SG', age: 25, height: '200 cm', weight: '97 kg', nationality: 'Bosnalı',    status: 'available', captain: false },
                { id: 212, number: 6,  name: 'Alberto Abalde',     pos: 'PF', age: 29, height: '200 cm', weight: '97 kg', nationality: 'İspanyol',   status: 'available', captain: false }
            ]
        },
        {
            id: 3,
            name: 'Panathinaikos',
            city: 'Atina',
            country: 'Yunanistan',
            arena: 'OAKA Arena',
            capacity: 19000,
            coach: 'Ergin Ataman',
            founded: 1919,
            code: 'PAO',
            players: [
                { id: 301, number: 3,  name: 'Kendrick Nunn',      pos: 'PG', age: 30, height: '188 cm', weight: '86 kg', nationality: 'Amerikalı',  status: 'available', captain: false },
                { id: 302, number: 5,  name: 'Juancho Hernangomez', pos: 'PF', age: 29, height: '206 cm', weight: '107 kg', nationality: 'İspanyol', status: 'available', captain: false },
                { id: 303, number: 11, name: 'Lorenzo Brown',      pos: 'PG', age: 33, height: '196 cm', weight: '90 kg', nationality: 'Kanadalı',   status: 'available', captain: false },
                { id: 304, number: 10, name: 'Scottie Wilbekin',   pos: 'SG', age: 33, height: '188 cm', weight: '84 kg', nationality: 'Amerikalı',  status: 'available', captain: true  },
                { id: 305, number: 24, name: 'Nikos Rogkavopoulos', pos: 'SG', age: 24, height: '200 cm', weight: '90 kg', nationality: 'Yunan',     status: 'available', captain: false },
                { id: 306, number: 2,  name: 'Georgios Papagiannis', pos: 'C', age: 28, height: '218 cm', weight: '120 kg', nationality: 'Yunan',    status: 'injured',   captain: false },
                { id: 307, number: 42, name: 'Mateusz Ponitka',    pos: 'SF', age: 32, height: '200 cm', weight: '100 kg', nationality: 'Polonyalı', status: 'available', captain: false },
                { id: 308, number: 21, name: 'Thomas Walkup',      pos: 'PG', age: 32, height: '191 cm', weight: '86 kg', nationality: 'Amerikalı',  status: 'available', captain: false },
                { id: 309, number: 8,  name: 'Zach Auguste',       pos: 'C',  age: 30, height: '211 cm', weight: '109 kg', nationality: 'Amerikalı', status: 'doubtful',  captain: false },
                { id: 310, number: 44, name: 'Kevin Punter',       pos: 'SG', age: 31, height: '193 cm', weight: '86 kg', nationality: 'Amerikalı',  status: 'available', captain: false },
                { id: 311, number: 7,  name: 'Ioannis Papapetrou', pos: 'SF', age: 32, height: '200 cm', weight: '97 kg', nationality: 'Yunan',      status: 'available', captain: false },
                { id: 312, number: 23, name: 'Dimitris Mavroeidis', pos: 'PF', age: 26, height: '207 cm', weight: '104 kg', nationality: 'Yunan',    status: 'available', captain: false }
            ]
        },
        {
            id: 4,
            name: 'FC Barcelona',
            city: 'Barselona',
            country: 'İspanya',
            arena: 'Palau Blaugrana',
            capacity: 7585,
            coach: 'Roger Grimau',
            founded: 1926,
            code: 'FCB',
            players: [
                { id: 401, number: 1,  name: 'Nicolas Laprovittola', pos: 'PG', age: 34, height: '186 cm', weight: '82 kg', nationality: 'Arjantinli', status: 'available', captain: false },
                { id: 402, number: 9,  name: 'Darius Thompson',    pos: 'PG', age: 33, height: '191 cm', weight: '86 kg', nationality: 'Amerikalı',  status: 'available', captain: false },
                { id: 403, number: 23, name: 'Jabari Parker',       pos: 'PF', age: 30, height: '203 cm', weight: '107 kg', nationality: 'Amerikalı', status: 'available', captain: false },
                { id: 404, number: 10, name: 'Victor Wembanyama',   pos: 'C',  age: 21, height: '224 cm', weight: '95 kg', nationality: 'Fransız',    status: 'available', captain: false },
                { id: 405, number: 22, name: 'Jan Vesely',          pos: 'C',  age: 34, height: '214 cm', weight: '114 kg', nationality: 'Çek',       status: 'available', captain: true  },
                { id: 406, number: 11, name: 'Rokas Jokubaitis',    pos: 'SG', age: 24, height: '191 cm', weight: '86 kg', nationality: 'Litvanyalı', status: 'doubtful',  captain: false },
                { id: 407, number: 6,  name: 'Dante Exum',          pos: 'SG', age: 30, height: '196 cm', weight: '90 kg', nationality: 'Avustralyalı', status: 'available', captain: false },
                { id: 408, number: 3,  name: 'Tomas Satoransky',    pos: 'PG', age: 33, height: '203 cm', weight: '97 kg', nationality: 'Çek',        status: 'available', captain: false },
                { id: 409, number: 15, name: 'Nnaji Zeke',          pos: 'PF', age: 24, height: '208 cm', weight: '109 kg', nationality: 'Amerikalı', status: 'available', captain: false },
                { id: 410, number: 7,  name: 'Willy Hernangomez',   pos: 'C',  age: 32, height: '213 cm', weight: '107 kg', nationality: 'İspanyol',  status: 'injured',   captain: false },
                { id: 411, number: 5,  name: 'Sertaç Şanlı',       pos: 'SF', age: 26, height: '205 cm', weight: '107 kg', nationality: 'Türk',      status: 'available', captain: false },
                { id: 412, number: 21, name: 'Nicolas Batum',       pos: 'SF', age: 36, height: '203 cm', weight: '104 kg', nationality: 'Fransız',   status: 'available', captain: false }
            ]
        },
        {
            id: 5,
            name: 'Fenerbahçe Beko',
            city: 'İstanbul',
            country: 'Türkiye',
            arena: 'Ülker Sports Arena',
            capacity: 13500,
            coach: 'Sarunas Jasikevicius',
            founded: 1907,
            code: 'FBK',
            players: [
                { id: 501, number: 1,  name: 'Scottie Wilbekin',   pos: 'PG', age: 33, height: '188 cm', weight: '84 kg', nationality: 'Amerikalı',  status: 'available', captain: false },
                { id: 502, number: 7,  name: 'Nigel Hayes-Davis',  pos: 'SF', age: 29, height: '201 cm', weight: '102 kg', nationality: 'Amerikalı', status: 'available', captain: false },
                { id: 503, number: 15, name: 'Muhammed Ali Duran', pos: 'SG', age: 27, height: '195 cm', weight: '86 kg', nationality: 'Türk',       status: 'available', captain: false },
                { id: 504, number: 10, name: 'James Nunnally',     pos: 'SG', age: 33, height: '196 cm', weight: '93 kg', nationality: 'Amerikalı',  status: 'doubtful',  captain: false },
                { id: 505, number: 11, name: 'Devin Booker',       pos: 'PF', age: 28, height: '198 cm', weight: '100 kg', nationality: 'Amerikalı', status: 'available', captain: false },
                { id: 506, number: 22, name: 'Ahmet Düverioğlu',   pos: 'C',  age: 24, height: '213 cm', weight: '107 kg', nationality: 'Türk',      status: 'available', captain: false },
                { id: 507, number: 5,  name: 'Obinna Eze',         pos: 'C',  age: 26, height: '215 cm', weight: '110 kg', nationality: 'Nijeryalı', status: 'available', captain: false },
                { id: 508, number: 3,  name: 'Marko Gudurić',      pos: 'SG', age: 29, height: '200 cm', weight: '97 kg', nationality: 'Sırp',       status: 'available', captain: true  },
                { id: 509, number: 44, name: 'De'Andre Jordan',     pos: 'C',  age: 36, height: '211 cm', weight: '120 kg', nationality: 'Amerikalı', status: 'injured',  captain: false },
                { id: 510, number: 23, name: 'Dyshawn Pierre',     pos: 'PF', age: 31, height: '201 cm', weight: '100 kg', nationality: 'Kanadalı',  status: 'available', captain: false },
                { id: 511, number: 8,  name: 'Martin Peterka',     pos: 'PG', age: 25, height: '189 cm', weight: '80 kg', nationality: 'Çek',        status: 'available', captain: false },
                { id: 512, number: 20, name: 'Bilal Çal',          pos: 'PF', age: 23, height: '204 cm', weight: '104 kg', nationality: 'Türk',      status: 'available', captain: false }
            ]
        },
        {
            id: 6,
            name: 'Olympiacos',
            city: 'Pire',
            country: 'Yunanistan',
            arena: 'Peace and Friendship Stadium',
            capacity: 14600,
            coach: 'Georgios Bartzokas',
            founded: 1931,
            code: 'OLY',
            players: [
                { id: 601, number: 10, name: 'Tyler Dorsey',       pos: 'SG', age: 28, height: '193 cm', weight: '86 kg', nationality: 'Amerikalı',  status: 'available', captain: false },
                { id: 602, number: 7,  name: 'Sasha Vezenkov',     pos: 'SF', age: 29, height: '206 cm', weight: '102 kg', nationality: 'Bulgar',    status: 'available', captain: true  },
                { id: 603, number: 5,  name: 'Shaquielle McKissic', pos: 'PG', age: 32, height: '191 cm', weight: '86 kg', nationality: 'Amerikalı', status: 'available', captain: false },
                { id: 604, number: 3,  name: 'Giannoulis Larentzakis', pos: 'SG', age: 26, height: '196 cm', weight: '90 kg', nationality: 'Yunan', status: 'doubtful',  captain: false },
                { id: 605, number: 1,  name: 'Vince Hunter',       pos: 'PF', age: 29, height: '203 cm', weight: '107 kg', nationality: 'Amerikalı', status: 'available', captain: false },
                { id: 606, number: 11, name: 'Moustapha Fall',     pos: 'C',  age: 30, height: '218 cm', weight: '107 kg', nationality: 'Senegalli', status: 'available', captain: false },
                { id: 607, number: 25, name: 'Kostas Papanikolaou', pos: 'SF', age: 34, height: '203 cm', weight: '100 kg', nationality: 'Yunan',    status: 'injured',   captain: false },
                { id: 608, number: 23, name: 'Desonta Bradford',   pos: 'SG', age: 27, height: '191 cm', weight: '86 kg', nationality: 'Amerikalı',  status: 'available', captain: false },
                { id: 609, number: 55, name: 'Joel Bolomboy',      pos: 'C',  age: 32, height: '208 cm', weight: '109 kg', nationality: 'Fransız',   status: 'available', captain: false },
                { id: 610, number: 8,  name: 'Georgios Printezis', pos: 'PF', age: 39, height: '203 cm', weight: '100 kg', nationality: 'Yunan',     status: 'available', captain: false },
                { id: 611, number: 9,  name: 'Nikola Milutinov',   pos: 'C',  age: 30, height: '216 cm', weight: '114 kg', nationality: 'Sırp',      status: 'available', captain: false },
                { id: 612, number: 21, name: 'Marius Grigonis',    pos: 'SF', age: 30, height: '200 cm', weight: '97 kg', nationality: 'Litvanyalı', status: 'available', captain: false }
            ]
        },
        {
            id: 7,
            name: 'LDLC ASVEL',
            city: 'Lyon-Villeurbanne',
            country: 'Fransa',
            arena: 'Astroballe',
            capacity: 5000,
            coach: 'Pierric Poupet',
            founded: 1948,
            code: 'ASV',
            players: [
                { id: 701, number: 1,  name: 'Elie Okobo',         pos: 'PG', age: 27, height: '190 cm', weight: '84 kg', nationality: 'Fransız',    status: 'available', captain: false },
                { id: 702, number: 5,  name: 'TJ Short',           pos: 'SG', age: 27, height: '191 cm', weight: '86 kg', nationality: 'Amerikalı',  status: 'available', captain: false },
                { id: 703, number: 10, name: 'David Lighty',       pos: 'SF', age: 36, height: '198 cm', weight: '97 kg', nationality: 'Amerikalı',  status: 'available', captain: true  },
                { id: 704, number: 7,  name: 'Paul Lacombe',       pos: 'SG', age: 35, height: '193 cm', weight: '86 kg', nationality: 'Fransız',    status: 'injured',   captain: false },
                { id: 705, number: 23, name: 'Zach Hankins',       pos: 'C',  age: 28, height: '211 cm', weight: '109 kg', nationality: 'Amerikalı', status: 'available', captain: false },
                { id: 706, number: 11, name: 'Norris Cole',        pos: 'PG', age: 36, height: '191 cm', weight: '86 kg', nationality: 'Amerikalı',  status: 'available', captain: false },
                { id: 707, number: 3,  name: 'Marcos Knight',      pos: 'PF', age: 30, height: '204 cm', weight: '104 kg', nationality: 'Amerikalı', status: 'doubtful',  captain: false },
                { id: 708, number: 22, name: 'Joffrey Lauvergne',  pos: 'C',  age: 33, height: '211 cm', weight: '114 kg', nationality: 'Fransız',   status: 'available', captain: false },
                { id: 709, number: 8,  name: 'Antoine Diot',       pos: 'PG', age: 36, height: '191 cm', weight: '82 kg', nationality: 'Fransız',    status: 'available', captain: false },
                { id: 710, number: 14, name: 'Yakuba Ouattara',    pos: 'SF', age: 25, height: '200 cm', weight: '97 kg', nationality: 'Fransız',    status: 'available', captain: false },
                { id: 711, number: 42, name: 'Charles Kahudi',     pos: 'SF', age: 36, height: '196 cm', weight: '90 kg', nationality: 'Fransız',    status: 'available', captain: false },
                { id: 712, number: 55, name: 'Deon Thompson',      pos: 'PF', age: 37, height: '203 cm', weight: '102 kg', nationality: 'Amerikalı', status: 'available', captain: false }
            ]
        },
        {
            id: 8,
            name: 'EA7 Olimpia Milano',
            city: 'Milano',
            country: 'İtalya',
            arena: 'Mediolanum Forum',
            capacity: 12700,
            coach: 'Ettore Messina',
            founded: 1936,
            code: 'OLM',
            players: [
                { id: 801, number: 11, name: 'Shabazz Napier',     pos: 'PG', age: 33, height: '185 cm', weight: '84 kg', nationality: 'Amerikalı',  status: 'available', captain: false },
                { id: 802, number: 1,  name: 'Zach LeDay',         pos: 'PF', age: 33, height: '203 cm', weight: '104 kg', nationality: 'Amerikalı', status: 'available', captain: false },
                { id: 803, number: 9,  name: 'Nikola Mirotic',     pos: 'PF', age: 33, height: '208 cm', weight: '109 kg', nationality: 'Crna Gora', status: 'available', captain: true  },
                { id: 804, number: 23, name: 'Chimezie Metu',      pos: 'C',  age: 28, height: '211 cm', weight: '109 kg', nationality: 'Nijeryalı', status: 'available', captain: false },
                { id: 805, number: 3,  name: 'Shavon Shields',     pos: 'SF', age: 31, height: '198 cm', weight: '100 kg', nationality: 'Amerikalı', status: 'doubtful',  captain: false },
                { id: 806, number: 7,  name: 'Giampaolo Ricci',    pos: 'SF', age: 33, height: '200 cm', weight: '97 kg', nationality: 'İtalyan',    status: 'available', captain: false },
                { id: 807, number: 13, name: 'Jordan Hall',        pos: 'PG', age: 27, height: '185 cm', weight: '79 kg', nationality: 'Amerikalı',  status: 'available', captain: false },
                { id: 808, number: 44, name: 'Troy Daniels',       pos: 'SG', age: 34, height: '196 cm', weight: '90 kg', nationality: 'Amerikalı',  status: 'available', captain: false },
                { id: 809, number: 5,  name: 'Jerian Grant',       pos: 'SG', age: 32, height: '191 cm', weight: '86 kg', nationality: 'Amerikalı',  status: 'injured',   captain: false },
                { id: 810, number: 22, name: 'Johannes Voigtmann', pos: 'C',  age: 32, height: '213 cm', weight: '107 kg', nationality: 'Alman',     status: 'available', captain: false },
                { id: 811, number: 8,  name: 'Brandon Davies',     pos: 'C',  age: 31, height: '208 cm', weight: '109 kg', nationality: 'Amerikalı', status: 'available', captain: false },
                { id: 812, number: 21, name: 'Kevin Pangos',       pos: 'PG', age: 32, height: '185 cm', weight: '79 kg', nationality: 'Kanadalı',   status: 'available', captain: false }
            ]
        }
    ];

    /* ------------------------------------------------------------------ */
    /* Uygulama Durumu                                                     */
    /* ------------------------------------------------------------------ */
    var state = {
        teams: [],
        selectedTeamId: null,
        teamFilter: 'ALL',
        posFilter: 'ALL',
        viewMode: 'table',
        teamSearch: '',
        rosterSearch: '',
        apiStatus: 'loading'
    };

    /* ------------------------------------------------------------------ */
    /* Yardımcı Fonksiyonlar                                               */
    /* ------------------------------------------------------------------ */
    function esc(str) {
        return String(str)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;');
    }

    function initials(name) {
        var parts = name.trim().split(' ');
        if (parts.length >= 2) {
            return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
        }
        return parts[0].substring(0, 2).toUpperCase();
    }

    function getContextRoot() {
        var path = window.location.pathname;
        var idx = path.indexOf('/', 1);
        if (idx === -1) return '';
        var seg = path.substring(0, idx);
        if (seg.indexOf('.') !== -1) return '';
        return seg;
    }

    function statusLabel(s) {
        if (s === 'available') return 'Hazır';
        if (s === 'injured')   return 'Sakatlandı';
        if (s === 'doubtful')  return 'Şüpheli';
        return s;
    }

    function posLabel(p) {
        var map = { PG: 'Oyun Kurucu', SG: 'Şut Gardı', SF: 'Küçük Forvet', PF: 'Güçlü Forvet', C: 'Pivot' };
        return map[p] || p;
    }

    /* ------------------------------------------------------------------ */
    /* API — Veri Çekme                                                    */
    /* ------------------------------------------------------------------ */
    function loadTeams() {
        var root = getContextRoot();
        var url = (root ? root : '') + '/api/teams';
        var controller = new AbortController();
        var timer = setTimeout(function () { controller.abort(); }, 2500);

        fetch(url, { signal: controller.signal })
            .then(function (res) {
                clearTimeout(timer);
                if (!res.ok) throw new Error('HTTP ' + res.status);
                return res.json();
            })
            .then(function (data) {
                state.teams = Array.isArray(data) ? data : (data.teams || []);
                state.apiStatus = 'live';
                updateApiStatusUI();
                render();
            })
            .catch(function () {
                clearTimeout(timer);
                state.teams = DEMO_TEAMS;
                state.apiStatus = 'demo';
                updateApiStatusUI();
                render();
            });
    }

    function updateApiStatusUI() {
        var dot  = document.getElementById('api-dot');
        var text = document.getElementById('api-text');
        if (!dot || !text) return;

        if (state.apiStatus === 'live') {
            dot.className  = 'status-dot live';
            text.textContent = 'Canlı API';
        } else if (state.apiStatus === 'demo') {
            dot.className  = 'status-dot demo';
            text.textContent = 'Demo Veri';
        } else {
            dot.className  = 'status-dot';
            text.textContent = 'Bağlanıyor...';
        }
    }

    /* ------------------------------------------------------------------ */
    /* Render — Takımlar Listesi                                           */
    /* ------------------------------------------------------------------ */
    function getFilteredTeams() {
        return state.teams.filter(function (t) {
            var matchCountry = state.teamFilter === 'ALL' || t.country === state.teamFilter;
            var q = state.teamSearch.toLowerCase();
            var matchSearch = !q ||
                t.name.toLowerCase().includes(q) ||
                t.city.toLowerCase().includes(q) ||
                (t.coach && t.coach.toLowerCase().includes(q));
            return matchCountry && matchSearch;
        });
    }

    function renderTeamList() {
        var ul    = document.getElementById('teams-list');
        var badge = document.getElementById('teams-count-badge');
        if (!ul) return;

        var filtered = getFilteredTeams();
        badge.textContent = filtered.length;

        if (filtered.length === 0) {
            ul.innerHTML = '<li class="empty-state">Eşleşen takım bulunamadı.</li>';
            return;
        }

        ul.innerHTML = filtered.map(function (t) {
            var isSel = t.id === state.selectedTeamId;
            return '<li class="team-item' + (isSel ? ' selected' : '') + '" role="option" aria-selected="' + isSel + '" data-team-id="' + t.id + '">' +
                '<div class="team-crest">' + esc(t.code || t.name.substring(0, 3)) + '</div>' +
                '<div class="team-item-body">' +
                    '<div class="team-item-name">' + esc(t.name) + '</div>' +
                    '<div class="team-item-meta">' + esc(t.city) + ' &bull; ' + esc(t.country) + '</div>' +
                '</div>' +
                '<svg class="team-item-arrow" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><polyline points="9 18 15 12 9 6"/></svg>' +
            '</li>';
        }).join('');

        ul.querySelectorAll('.team-item').forEach(function (li) {
            li.addEventListener('click', function () {
                selectTeam(Number(li.getAttribute('data-team-id')));
            });
        });
    }

    /* ------------------------------------------------------------------ */
    /* Render — Kulüp Künyesi                                              */
    /* ------------------------------------------------------------------ */
    function renderTeamHero(team) {
        var container = document.getElementById('team-hero-container');
        if (!container) return;

        container.innerHTML =
            '<div class="club-card">' +
                '<div class="club-card-top">' +
                    '<div class="club-top-row">' +
                        '<div class="club-badge">' + esc(team.code || team.name.substring(0, 3)) + '</div>' +
                        '<div class="club-info">' +
                            '<h2 class="club-name">' + esc(team.name) + '</h2>' +
                            '<div class="club-sub">' + esc(team.city) + ', ' + esc(team.country) + '</div>' +
                        '</div>' +
                        '<span class="club-code">' + esc(team.code || '') + '</span>' +
                    '</div>' +
                '</div>' +
                '<div class="club-card-body">' +
                    '<div class="club-meta-grid">' +
                        metaItem('Ev Sahibi Salonu', team.arena || '-') +
                        metaItem('Kapasite', team.capacity ? team.capacity.toLocaleString('tr-TR') + ' kişi' : '-') +
                        metaItem('Baş Antrenör', team.coach || '-') +
                        metaItem('Kuruluş Yılı', team.founded || '-') +
                    '</div>' +
                '</div>' +
            '</div>';
    }

    function metaItem(label, value) {
        return '<div class="meta-item"><div class="meta-label">' + esc(label) + '</div><div class="meta-value">' + esc(String(value)) + '</div></div>';
    }

    /* ------------------------------------------------------------------ */
    /* Render — İstatistik Şeridi                                          */
    /* ------------------------------------------------------------------ */
    function renderTeamStats(team) {
        var strip = document.getElementById('team-stats-strip');
        if (!strip) return;

        var players = team.players || [];
        var total   = players.length;
        var avail   = players.filter(function (p) { return p.status === 'available'; }).length;
        var injured = players.filter(function (p) { return p.status === 'injured'; }).length;
        var captains = players.filter(function (p) { return p.captain; });
        var captainName = captains.length > 0 ? captains[0].name.split(' ').pop() : '-';

        strip.innerHTML =
            statCard(String(total),    'Oyuncu', 'Toplam kadro') +
            statCard(String(avail),    'Hazır',  'Oynayabilir') +
            statCard(String(injured),  'Sakatlık', 'Aktif sakatlık') +
            statCard(captainName,       'Kaptan', 'Takım kaptanı');
    }

    function statCard(num, label, sub) {
        return '<div class="stat-card"><div class="stat-label">' + esc(label) + '</div><div class="stat-num">' + esc(num) + '</div><div class="stat-sub">' + esc(sub) + '</div></div>';
    }

    /* ------------------------------------------------------------------ */
    /* Render — Kadro (Tablo)                                              */
    /* ------------------------------------------------------------------ */
    function getFilteredPlayers(team) {
        var players = team.players || [];
        return players.filter(function (p) {
            var matchPos = state.posFilter === 'ALL' || p.pos === state.posFilter;
            var q = state.rosterSearch.toLowerCase();
            var matchSearch = !q || p.name.toLowerCase().includes(q) || (p.nationality && p.nationality.toLowerCase().includes(q));
            return matchPos && matchSearch;
        });
    }

    function renderRosterTable(players) {
        if (players.length === 0) {
            return '<div class="empty-state">Filtre koşuluna uyan oyuncu bulunamadı.</div>';
        }

        var rows = players.map(function (p) {
            var ini  = initials(p.name);
            var cpt  = p.captain ? '<span class="cpt-badge">C</span>' : '';
            var pos  = '<span class="pos-badge ' + esc(p.pos) + '">' + esc(p.pos) + '</span>';
            var stat = '<span class="status-badge ' + esc(p.status) + '"><span class="dot"></span>' + statusLabel(p.status) + '</span>';

            return '<tr onclick="window.__teamsApp.openModal(' + p.id + ')" style="cursor:pointer">' +
                '<td class="col-no">' + esc(String(p.number)) + '</td>' +
                '<td><div class="player-cell">' +
                    '<div class="player-initials">' + ini + '</div>' +
                    '<div>' +
                        '<div class="player-name-main">' + esc(p.name) + cpt + '</div>' +
                        '<div style="font-size:0.75rem;color:var(--text-light)">' + esc(p.nationality || '') + '</div>' +
                    '</div>' +
                '</div></td>' +
                '<td>' + pos + '</td>' +
                '<td>' + esc(p.height || '-') + '</td>' +
                '<td>' + esc(p.weight || '-') + '</td>' +
                '<td>' + esc(String(p.age || '-')) + '</td>' +
                '<td>' + stat + '</td>' +
            '</tr>';
        }).join('');

        return '<div class="table-scroll"><table class="roster-table">' +
            '<thead><tr>' +
                '<th class="col-no">#</th>' +
                '<th>Oyuncu</th>' +
                '<th>Mevki</th>' +
                '<th>Boy</th>' +
                '<th>Kilo</th>' +
                '<th>Yaş</th>' +
                '<th>Durum</th>' +
            '</tr></thead>' +
            '<tbody>' + rows + '</tbody>' +
        '</table></div>';
    }

    /* ------------------------------------------------------------------ */
    /* Render — Kadro (Kartlar)                                            */
    /* ------------------------------------------------------------------ */
    function renderRosterCards(players) {
        if (players.length === 0) {
            return '<div class="empty-state">Filtre koşuluna uyan oyuncu bulunamadı.</div>';
        }

        var cards = players.map(function (p) {
            var pos  = '<span class="pos-badge ' + esc(p.pos) + '">' + esc(p.pos) + '</span>';
            var cpt  = p.captain ? '<span class="cpt-badge">C</span>' : '';
            return '<div class="player-card" onclick="window.__teamsApp.openModal(' + p.id + ')" style="cursor:pointer">' +
                '<div class="card-top-line">' +
                    '<span class="card-jersey">#' + esc(String(p.number)) + '</span>' +
                    pos + (p.captain ? cpt : '') +
                '</div>' +
                '<div class="card-player-name">' + esc(p.name) + '</div>' +
                '<div class="card-pos-line">' + posLabel(p.pos) + '</div>' +
                '<div class="card-details">' +
                    '<div class="card-detail-row"><span>Boy</span><span>' + esc(p.height || '-') + '</span></div>' +
                    '<div class="card-detail-row"><span>Kilo</span><span>' + esc(p.weight || '-') + '</span></div>' +
                    '<div class="card-detail-row"><span>Yaş</span><span>' + esc(String(p.age || '-')) + '</span></div>' +
                    '<div class="card-detail-row"><span>Uyruk</span><span>' + esc(p.nationality || '-') + '</span></div>' +
                '</div>' +
            '</div>';
        }).join('');

        return '<div class="cards-grid">' + cards + '</div>';
    }

    /* ------------------------------------------------------------------ */
    /* Render — Kadro Analizi                                              */
    /* ------------------------------------------------------------------ */
    function renderInsights(team) {
        var box = document.getElementById('team-insights-card');
        if (!box) return;

        var players = team.players || [];
        var positions = ['PG', 'SG', 'SF', 'PF', 'C'];
        var counts = {};
        positions.forEach(function (p) { counts[p] = 0; });
        players.forEach(function (p) { if (counts[p.pos] !== undefined) counts[p.pos]++; });
        var total = players.length || 1;

        var distSegments = positions.map(function (pos) {
            var pct = Math.round(counts[pos] / total * 100);
            return '<div class="dist-segment ' + pos.toLowerCase() + '" style="width:' + pct + '%" title="' + pos + ': ' + counts[pos] + '"></div>';
        }).join('');

        var distLegend = positions.map(function (pos) {
            return '<span class="legend-dot ' + pos.toLowerCase() + '"></span>' + pos + ': ' + counts[pos];
        }).join(' &nbsp;');

        // Durum dağılımı
        var statuses = {
            available: players.filter(function (p) { return p.status === 'available'; }).length,
            injured:   players.filter(function (p) { return p.status === 'injured'; }).length,
            doubtful:  players.filter(function (p) { return p.status === 'doubtful'; }).length
        };

        // Ortalama yaş
        var ages = players.filter(function (p) { return p.age; }).map(function (p) { return p.age; });
        var avgAge = ages.length > 0 ? (ages.reduce(function (a, b) { return a + b; }, 0) / ages.length).toFixed(1) : '-';

        box.innerHTML =
            '<div class="analysis-col">' +
                '<div class="analysis-col-title">Mevki Dağılımı</div>' +
                '<div class="dist-bar">' + distSegments + '</div>' +
                '<div class="dist-legend">' + distLegend + '</div>' +
            '</div>' +
            '<div class="analysis-col">' +
                '<div class="analysis-col-title">Kadro Bilgileri</div>' +
                '<ul class="analysis-list">' +
                    '<li>Ortalama yaş: <strong>' + avgAge + '</strong></li>' +
                    '<li>Hazır oyuncu: <strong>' + statuses.available + '</strong></li>' +
                    '<li>Sakatlık: <strong>' + statuses.injured + '</strong></li>' +
                    '<li>Şüpheli: <strong>' + statuses.doubtful + '</strong></li>' +
                '</ul>' +
            '</div>';
    }

    /* ------------------------------------------------------------------ */
    /* Takım Seçimi                                                        */
    /* ------------------------------------------------------------------ */
    function selectTeam(id) {
        state.selectedTeamId = id;
        state.posFilter = 'ALL';
        state.rosterSearch = '';

        // Pozisyon tab sıfırla
        document.querySelectorAll('.pos-tab-btn').forEach(function (btn) {
            btn.classList.toggle('active', btn.getAttribute('data-pos') === 'ALL');
        });
        // Roster arama sıfırla
        var ri = document.getElementById('roster-search-input');
        if (ri) ri.value = '';

        renderTeamList();
        renderDetail();
    }

    function renderDetail() {
        var team = state.teams.find(function (t) { return t.id === state.selectedTeamId; });
        if (!team) return;

        renderTeamHero(team);
        renderTeamStats(team);
        renderRoster(team);
        renderInsights(team);
    }

    function renderRoster(team) {
        var area  = document.getElementById('roster-content-area');
        var badge = document.getElementById('roster-count-badge');
        if (!area) return;

        var players = getFilteredPlayers(team);
        badge.textContent = players.length;

        if (state.viewMode === 'table') {
            area.innerHTML = renderRosterTable(players);
        } else {
            area.innerHTML = renderRosterCards(players);
        }
    }

    /* ------------------------------------------------------------------ */
    /* Oyuncu Modalı                                                       */
    /* ------------------------------------------------------------------ */
    function openModal(playerId) {
        var team = state.teams.find(function (t) { return t.id === state.selectedTeamId; });
        if (!team) return;
        var player = (team.players || []).find(function (p) { return p.id === playerId; });
        if (!player) return;

        var modal   = document.getElementById('player-modal');
        var content = document.getElementById('modal-player-content');
        var title   = document.getElementById('modal-player-title');
        if (!modal || !content) return;

        if (title) title.textContent = player.name;

        var pos   = '<span class="pos-badge ' + esc(player.pos) + '">' + esc(player.pos) + '</span>';
        var stat  = '<span class="status-badge ' + esc(player.status) + '"><span class="dot"></span>' + statusLabel(player.status) + '</span>';
        var cpt   = player.captain ? ' &nbsp;<span class="cpt-badge">Kaptan</span>' : '';

        content.innerHTML =
            '<div class="modal-player-hero">' +
                '<div class="modal-jersey-num">#' + esc(String(player.number)) + '</div>' +
                '<div class="modal-player-info">' +
                    '<h3>' + esc(player.name) + cpt + '</h3>' +
                    '<div style="margin-top:0.5rem;display:flex;align-items:center;gap:0.5rem">' + pos + stat + '</div>' +
                    '<div style="font-size:0.85rem;color:var(--text-light);margin-top:0.4rem">' + posLabel(player.pos) + '</div>' +
                '</div>' +
            '</div>' +
            '<div class="modal-data-grid">' +
                modalField('Forma No', '#' + player.number) +
                modalField('Boy', player.height || '-') +
                modalField('Kilo', player.weight || '-') +
                modalField('Yaş', player.age || '-') +
                modalField('Uyruk', player.nationality || '-') +
                modalField('Mevki', posLabel(player.pos)) +
            '</div>' +
            '<p class="modal-note">Detaylı istatistikler canlı API bağlantısıyla görüntülenebilir.</p>';

        modal.classList.add('open');
        document.body.style.overflow = 'hidden';

        // Kapat tuşu — ESC
        function onKey(e) {
            if (e.key === 'Escape') closeModal();
        }
        document.addEventListener('keydown', onKey, { once: true });
    }

    function modalField(label, value) {
        return '<div><div class="modal-field-label">' + esc(label) + '</div><div class="modal-field-val">' + esc(String(value)) + '</div></div>';
    }

    function closeModal() {
        var modal = document.getElementById('player-modal');
        if (modal) modal.classList.remove('open');
        document.body.style.overflow = '';
    }

    /* ------------------------------------------------------------------ */
    /* Genel Render                                                        */
    /* ------------------------------------------------------------------ */
    function render() {
        renderTeamList();
        if (state.selectedTeamId) {
            renderDetail();
        }
    }

    /* ------------------------------------------------------------------ */
    /* Olay Dinleyicileri                                                  */
    /* ------------------------------------------------------------------ */
    function attachListeners() {
        // Takım arama
        var teamSearch = document.getElementById('team-search-input');
        if (teamSearch) {
            teamSearch.addEventListener('input', function () {
                state.teamSearch = teamSearch.value;
                renderTeamList();
            });
        }

        // Ülke filtreleri
        var countryTabs = document.querySelector('.country-tabs');
        if (countryTabs) {
            countryTabs.addEventListener('click', function (e) {
                var btn = e.target.closest('button[data-country]');
                if (!btn) return;
                state.teamFilter = btn.getAttribute('data-country');
                countryTabs.querySelectorAll('button').forEach(function (b) {
                    b.classList.toggle('active', b === btn);
                });
                renderTeamList();
            });
        }

        // Pozisyon filtreleri
        var posTabs = document.querySelector('.pos-tabs');
        if (posTabs) {
            posTabs.addEventListener('click', function (e) {
                var btn = e.target.closest('.pos-tab-btn');
                if (!btn) return;
                state.posFilter = btn.getAttribute('data-pos');
                posTabs.querySelectorAll('.pos-tab-btn').forEach(function (b) {
                    b.classList.toggle('active', b === btn);
                });
                if (state.selectedTeamId) {
                    var team = state.teams.find(function (t) { return t.id === state.selectedTeamId; });
                    if (team) renderRoster(team);
                }
            });
        }

        // Görünüm değiştirici
        var viewToggle = document.querySelector('.view-toggle');
        if (viewToggle) {
            viewToggle.addEventListener('click', function (e) {
                var btn = e.target.closest('.view-btn');
                if (!btn) return;
                state.viewMode = btn.getAttribute('data-view');
                viewToggle.querySelectorAll('.view-btn').forEach(function (b) {
                    b.classList.toggle('active', b === btn);
                });
                if (state.selectedTeamId) {
                    var team = state.teams.find(function (t) { return t.id === state.selectedTeamId; });
                    if (team) renderRoster(team);
                }
            });
        }

        // Oyuncu arama
        var rosterSearch = document.getElementById('roster-search-input');
        if (rosterSearch) {
            rosterSearch.addEventListener('input', function () {
                state.rosterSearch = rosterSearch.value;
                if (state.selectedTeamId) {
                    var team = state.teams.find(function (t) { return t.id === state.selectedTeamId; });
                    if (team) renderRoster(team);
                }
            });
        }

        // Modal kapat - buton
        var closeBtn = document.getElementById('btn-close-modal');
        if (closeBtn) {
            closeBtn.addEventListener('click', closeModal);
        }

        // Modal kapat - overlay tıklama
        var modal = document.getElementById('player-modal');
        if (modal) {
            modal.addEventListener('click', function (e) {
                if (e.target === modal) closeModal();
            });
        }

        // API yenile
        var refreshBtn = document.getElementById('btn-refresh-api');
        if (refreshBtn) {
            refreshBtn.addEventListener('click', function () {
                state.apiStatus = 'loading';
                updateApiStatusUI();
                loadTeams();
            });
        }
    }

    /* ------------------------------------------------------------------ */
    /* Global Erişim (modal için)                                         */
    /* ------------------------------------------------------------------ */
    window.__teamsApp = {
        openModal: openModal
    };

    /* ------------------------------------------------------------------ */
    /* Başlatma                                                            */
    /* ------------------------------------------------------------------ */
    document.addEventListener('DOMContentLoaded', function () {
        attachListeners();
        loadTeams();

        // İlk takımı seç
        setTimeout(function () {
            if (state.teams.length > 0 && !state.selectedTeamId) {
                selectTeam(state.teams[0].id);
            }
        }, 100);
    });

})();
