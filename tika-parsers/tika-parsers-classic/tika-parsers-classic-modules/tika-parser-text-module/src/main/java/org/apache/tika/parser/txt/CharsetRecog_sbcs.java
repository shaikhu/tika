// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
/*
 ****************************************************************************
 * Copyright (C) 2005-2013, International Business Machines Corporation and *
 * others. All Rights Reserved.                                             *
 ************************************************************************** *
 *
 */

package org.apache.tika.parser.txt;

/**
 * This class recognizes single-byte encodings. Because the encoding scheme is so
 * simple, language statistics are used to do the matching.
 * <p/>
 * The Recognizer works by first mapping from bytes in the encoding under test
 * into that Recognizer's ngram space. Normally this means performing a
 * lowercase, and excluding codepoints that don't correspond to numbers of
 * letters. (Accented letters may or may not be ignored or normalised, depending
 * on the needs of the ngrams)
 * Then, ngram analysis is run against the transformed text, and a confidence
 * is calculated.
 * <p/>
 * For many of our Recognizers, we have one ngram set per language in each
 * encoding, and do a simultanious language+charset detection.
 * <p/>
 * When adding new Recognizers, the easiest way is to byte map to an existing
 * encoding for which we have ngrams, excluding non text, and re-use the ngrams.
 *
 * @internal
 */
abstract class CharsetRecog_sbcs extends CharsetRecognizer {

    /* (non-Javadoc)
     * @see com.ibm.icu.text.CharsetRecognizer#getName()
     */
    abstract String getName();

    int match(CharsetDetector det, int[] ngrams, byte[] byteMap) {
        return match(det, ngrams, byteMap, (byte) 0x20);
    }

    int match(CharsetDetector det, int[] ngrams, byte[] byteMap, byte spaceChar) {
        NGramParser parser = new NGramParser(ngrams, byteMap);
        return parser.parse(det, spaceChar);
    }

    int matchIBM420(CharsetDetector det, int[] ngrams, byte[] byteMap, byte spaceChar) {
        NGramParser_IBM420 parser = new NGramParser_IBM420(ngrams, byteMap);
        return parser.parse(det, spaceChar);
    }

    static class NGramParser {
        //        private static final int N_GRAM_SIZE = 3;
        private static final int N_GRAM_MASK = 0xFFFFFF;

        protected int byteIndex = 0;
        protected byte[] byteMap;
        protected byte spaceChar;
        private int ngram = 0;
        private int[] ngramList;
        private int ngramCount;
        private int hitCount;

        public NGramParser(int[] theNgramList, byte[] theByteMap) {
            ngramList = theNgramList;
            byteMap = theByteMap;

            ngram = 0;

            ngramCount = hitCount = 0;
        }

        /*
         * Binary search for value in table, which must have exactly 64 entries.
         */
        private static int search(int[] table, int value) {
            int index = 0;

            if (table[index + 32] <= value) {
                index += 32;
            }

            if (table[index + 16] <= value) {
                index += 16;
            }

            if (table[index + 8] <= value) {
                index += 8;
            }

            if (table[index + 4] <= value) {
                index += 4;
            }

            if (table[index + 2] <= value) {
                index += 2;
            }

            if (table[index + 1] <= value) {
                index += 1;
            }

            if (table[index] > value) {
                index -= 1;
            }

            if (index < 0 || table[index] != value) {
                return -1;
            }

            return index;
        }

        private void lookup(int thisNgram) {
            ngramCount += 1;

            if (search(ngramList, thisNgram) >= 0) {
                hitCount += 1;
            }

        }

        protected void addByte(int b) {
            ngram = ((ngram << 8) + (b & 0xFF)) & N_GRAM_MASK;
            lookup(ngram);
        }

        private int nextByte(CharsetDetector det) {
            if (byteIndex >= det.fInputLen) {
                return -1;
            }

            return det.fInputBytes[byteIndex++] & 0xFF;
        }

        protected void parseCharacters(CharsetDetector det) {
            int b;
            boolean ignoreSpace = false;

            while ((b = nextByte(det)) >= 0) {
                byte mb = byteMap[b];

                // TODO: 0x20 might not be a space in all character sets...
                if (mb != 0) {
                    if (!(mb == spaceChar && ignoreSpace)) {
                        addByte(mb);
                    }

                    ignoreSpace = (mb == spaceChar);
                }
            }

        }

        public int parse(CharsetDetector det) {
            return parse(det, (byte) 0x20);
        }

        public int parse(CharsetDetector det, byte spaceCh) {

            this.spaceChar = spaceCh;

            parseCharacters(det);

            // TODO: Is this OK? The buffer could have ended in the middle of a word...
            addByte(spaceChar);

            double rawPercent = (double) hitCount / (double) ngramCount;

//                if (rawPercent <= 2.0) {
//                    return 0;
//                }

            // TODO - This is a bit of a hack to take care of a case
            // were we were getting a confidence of 135...
            if (rawPercent > 0.33) {
                return 98;
            }

            return (int) (rawPercent * 300.0);
        }
    }

    static class NGramParser_IBM420 extends NGramParser {
        protected static byte[] unshapeMap = {
/*                 -0           -1           -2           -3
 -4           -5           -6           -7           -8
  -9           -A           -B           -C           -D           -E           -F   */
/* 0- */    (byte) 0x40, (byte) 0x40, (byte) 0x40, (byte) 0x40, (byte) 0x40, (byte) 0x40,
                (byte) 0x40, (byte) 0x40, (byte) 0x40, (byte) 0x40, (byte) 0x40, (byte) 0x40,
                (byte) 0x40, (byte) 0x40, (byte) 0x40, (byte) 0x40,
/* 1- */    (byte) 0x40, (byte) 0x40, (byte) 0x40, (byte) 0x40, (byte) 0x40, (byte) 0x40,
                (byte) 0x40, (byte) 0x40, (byte) 0x40, (byte) 0x40, (byte) 0x40, (byte) 0x40,
                (byte) 0x40, (byte) 0x40, (byte) 0x40, (byte) 0x40,
/* 2- */    (byte) 0x40, (byte) 0x40, (byte) 0x40, (byte) 0x40, (byte) 0x40, (byte) 0x40,
                (byte) 0x40, (byte) 0x40, (byte) 0x40, (byte) 0x40, (byte) 0x40, (byte) 0x40,
                (byte) 0x40, (byte) 0x40, (byte) 0x40, (byte) 0x40,
/* 3- */    (byte) 0x40, (byte) 0x40, (byte) 0x40, (byte) 0x40, (byte) 0x40, (byte) 0x40,
                (byte) 0x40, (byte) 0x40, (byte) 0x40, (byte) 0x40, (byte) 0x40, (byte) 0x40,
                (byte) 0x40, (byte) 0x40, (byte) 0x40, (byte) 0x40,
/* 4- */    (byte) 0x40, (byte) 0x40, (byte) 0x42, (byte) 0x42, (byte) 0x44, (byte) 0x45,
                (byte) 0x46, (byte) 0x47, (byte) 0x47, (byte) 0x49, (byte) 0x4A, (byte) 0x4B,
                (byte) 0x4C, (byte) 0x4D, (byte) 0x4E, (byte) 0x4F,
/* 5- */    (byte) 0x50, (byte) 0x49, (byte) 0x52, (byte) 0x53, (byte) 0x54, (byte) 0x55,
                (byte) 0x56, (byte) 0x56, (byte) 0x58, (byte) 0x58, (byte) 0x5A, (byte) 0x5B,
                (byte) 0x5C, (byte) 0x5D, (byte) 0x5E, (byte) 0x5F,
/* 6- */    (byte) 0x60, (byte) 0x61, (byte) 0x62, (byte) 0x63, (byte) 0x63, (byte) 0x65,
                (byte) 0x65, (byte) 0x67, (byte) 0x67, (byte) 0x69, (byte) 0x6A, (byte) 0x6B,
                (byte) 0x6C, (byte) 0x6D, (byte) 0x6E, (byte) 0x6F,
/* 7- */    (byte) 0x69, (byte) 0x71, (byte) 0x71, (byte) 0x73, (byte) 0x74, (byte) 0x75,
                (byte) 0x76, (byte) 0x77, (byte) 0x77, (byte) 0x79, (byte) 0x7A, (byte) 0x7B,
                (byte) 0x7C, (byte) 0x7D, (byte) 0x7E, (byte) 0x7F,
/* 8- */    (byte) 0x80, (byte) 0x81, (byte) 0x82, (byte) 0x83, (byte) 0x84, (byte) 0x85,
                (byte) 0x86, (byte) 0x87, (byte) 0x88, (byte) 0x89, (byte) 0x80, (byte) 0x8B,
                (byte) 0x8B, (byte) 0x8D, (byte) 0x8D, (byte) 0x8F,
/* 9- */    (byte) 0x90, (byte) 0x91, (byte) 0x92, (byte) 0x93, (byte) 0x94, (byte) 0x95,
                (byte) 0x96, (byte) 0x97, (byte) 0x98, (byte) 0x99, (byte) 0x9A, (byte) 0x9A,
                (byte) 0x9A, (byte) 0x9A, (byte) 0x9E, (byte) 0x9E,
/* A- */    (byte) 0x9E, (byte) 0xA1, (byte) 0xA2, (byte) 0xA3, (byte) 0xA4, (byte) 0xA5,
                (byte) 0xA6, (byte) 0xA7, (byte) 0xA8, (byte) 0xA9, (byte) 0x9E, (byte) 0xAB,
                (byte) 0xAB, (byte) 0xAD, (byte) 0xAD, (byte) 0xAF,
/* B- */    (byte) 0xAF, (byte) 0xB1, (byte) 0xB2, (byte) 0xB3, (byte) 0xB4, (byte) 0xB5,
                (byte) 0xB6, (byte) 0xB7, (byte) 0xB8, (byte) 0xB9, (byte) 0xB1, (byte) 0xBB,
                (byte) 0xBB, (byte) 0xBD, (byte) 0xBD, (byte) 0xBF,
/* C- */    (byte) 0xC0, (byte) 0xC1, (byte) 0xC2, (byte) 0xC3, (byte) 0xC4, (byte) 0xC5,
                (byte) 0xC6, (byte) 0xC7, (byte) 0xC8, (byte) 0xC9, (byte) 0xCA, (byte) 0xBF,
                (byte) 0xCC, (byte) 0xBF, (byte) 0xCE, (byte) 0xCF,
/* D- */    (byte) 0xD0, (byte) 0xD1, (byte) 0xD2, (byte) 0xD3, (byte) 0xD4, (byte) 0xD5,
                (byte) 0xD6, (byte) 0xD7, (byte) 0xD8, (byte) 0xD9, (byte) 0xDA, (byte) 0xDA,
                (byte) 0xDC, (byte) 0xDC, (byte) 0xDC, (byte) 0xDF,
/* E- */    (byte) 0xE0, (byte) 0xE1, (byte) 0xE2, (byte) 0xE3, (byte) 0xE4, (byte) 0xE5,
                (byte) 0xE6, (byte) 0xE7, (byte) 0xE8, (byte) 0xE9, (byte) 0xEA, (byte) 0xEB,
                (byte) 0xEC, (byte) 0xED, (byte) 0xEE, (byte) 0xEF,
/* F- */    (byte) 0xF0, (byte) 0xF1, (byte) 0xF2, (byte) 0xF3, (byte) 0xF4, (byte) 0xF5,
                (byte) 0xF6, (byte) 0xF7, (byte) 0xF8, (byte) 0xF9, (byte) 0xFA, (byte) 0xFB,
                (byte) 0xFC, (byte) 0xFD, (byte) 0xFE, (byte) 0xFF,};
        private byte alef = 0x00;


        public NGramParser_IBM420(int[] theNgramList, byte[] theByteMap) {
            super(theNgramList, theByteMap);
        }

        private byte isLamAlef(byte b) {
            if (b == (byte) 0xb2 || b == (byte) 0xb3) {
                return (byte) 0x47;
            } else if (b == (byte) 0xb4 || b == (byte) 0xb5) {
                return (byte) 0x49;
            } else if (b == (byte) 0xb8 || b == (byte) 0xb9) {
                return (byte) 0x56;
            } else {
                return (byte) 0x00;
            }
        }

        /*
         * Arabic shaping needs to be done manually. Cannot call ArabicShaping class
         * because CharsetDetector is dealing with bytes not Unicode code points. We could
         * convert the bytes to Unicode code points but that would leave us dependent
         * on CharsetICU which we try to avoid. IBM420 converter amongst different versions
         * of JDK can produce different results and therefore is also avoided.
         */
        private int nextByte(CharsetDetector det) {
            if (byteIndex >= det.fInputLen || det.fInputBytes[byteIndex] == 0) {
                return -1;
            }
            int next;

            alef = isLamAlef(det.fInputBytes[byteIndex]);
            if (alef != (byte) 0x00) {
                next = 0xB1 & 0xFF;
            } else {
                next = unshapeMap[det.fInputBytes[byteIndex] & 0xFF] & 0xFF;
            }

            byteIndex++;

            return next;
        }

        protected void parseCharacters(CharsetDetector det) {
            int b;
            boolean ignoreSpace = false;

            while ((b = nextByte(det)) >= 0) {
                byte mb = byteMap[b];

                // TODO: 0x20 might not be a space in all character sets...
                if (mb != 0) {
                    if (!(mb == spaceChar && ignoreSpace)) {
                        addByte(mb);
                    }

                    ignoreSpace = (mb == spaceChar);
                }
                if (alef != (byte) 0x00) {
                    mb = byteMap[alef & 0xFF];

                    // TODO: 0x20 might not be a space in all character sets...
                    if (mb != 0) {
                        if (!(mb == spaceChar && ignoreSpace)) {
                            addByte(mb);
                        }

                        ignoreSpace = (mb == spaceChar);
                    }

                }
            }
        }
    }

    static class NGramsPlusLang {
        int[] fNGrams;
        String fLang;

        NGramsPlusLang(String la, int[] ng) {
            fLang = la;
            fNGrams = ng;
        }
    }

    static class CharsetRecog_8859_1 extends CharsetRecog_sbcs {
        protected static byte[] byteMap =
                {(byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x00, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x61,
                        (byte) 0x62, (byte) 0x63, (byte) 0x64, (byte) 0x65, (byte) 0x66,
                        (byte) 0x67, (byte) 0x68, (byte) 0x69, (byte) 0x6A, (byte) 0x6B,
                        (byte) 0x6C, (byte) 0x6D, (byte) 0x6E, (byte) 0x6F, (byte) 0x70,
                        (byte) 0x71, (byte) 0x72, (byte) 0x73, (byte) 0x74, (byte) 0x75,
                        (byte) 0x76, (byte) 0x77, (byte) 0x78, (byte) 0x79, (byte) 0x7A,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x61, (byte) 0x62, (byte) 0x63, (byte) 0x64,
                        (byte) 0x65, (byte) 0x66, (byte) 0x67, (byte) 0x68, (byte) 0x69,
                        (byte) 0x6A, (byte) 0x6B, (byte) 0x6C, (byte) 0x6D, (byte) 0x6E,
                        (byte) 0x6F, (byte) 0x70, (byte) 0x71, (byte) 0x72, (byte) 0x73,
                        (byte) 0x74, (byte) 0x75, (byte) 0x76, (byte) 0x77, (byte) 0x78,
                        (byte) 0x79, (byte) 0x7A, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0xAA,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0xB5, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0xBA, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0xE0, (byte) 0xE1, (byte) 0xE2, (byte) 0xE3,
                        (byte) 0xE4, (byte) 0xE5, (byte) 0xE6, (byte) 0xE7, (byte) 0xE8,
                        (byte) 0xE9, (byte) 0xEA, (byte) 0xEB, (byte) 0xEC, (byte) 0xED,
                        (byte) 0xEE, (byte) 0xEF, (byte) 0xF0, (byte) 0xF1, (byte) 0xF2,
                        (byte) 0xF3, (byte) 0xF4, (byte) 0xF5, (byte) 0xF6, (byte) 0x20,
                        (byte) 0xF8, (byte) 0xF9, (byte) 0xFA, (byte) 0xFB, (byte) 0xFC,
                        (byte) 0xFD, (byte) 0xFE, (byte) 0xDF, (byte) 0xE0, (byte) 0xE1,
                        (byte) 0xE2, (byte) 0xE3, (byte) 0xE4, (byte) 0xE5, (byte) 0xE6,
                        (byte) 0xE7, (byte) 0xE8, (byte) 0xE9, (byte) 0xEA, (byte) 0xEB,
                        (byte) 0xEC, (byte) 0xED, (byte) 0xEE, (byte) 0xEF, (byte) 0xF0,
                        (byte) 0xF1, (byte) 0xF2, (byte) 0xF3, (byte) 0xF4, (byte) 0xF5,
                        (byte) 0xF6, (byte) 0x20, (byte) 0xF8, (byte) 0xF9, (byte) 0xFA,
                        (byte) 0xFB, (byte) 0xFC, (byte) 0xFD, (byte) 0xFE, (byte) 0xFF,};


        private static NGramsPlusLang[] ngrams_8859_1 = new NGramsPlusLang[]{
                new NGramsPlusLang("da",
                        new int[]{0x206166, 0x206174, 0x206465, 0x20656E, 0x206572, 0x20666F,
                                0x206861, 0x206920, 0x206D65, 0x206F67, 0x2070E5, 0x207369,
                                0x207374, 0x207469, 0x207669, 0x616620, 0x616E20, 0x616E64,
                                0x617220, 0x617420, 0x646520, 0x64656E, 0x646572, 0x646574,
                                0x652073, 0x656420, 0x656465, 0x656E20, 0x656E64, 0x657220,
                                0x657265, 0x657320, 0x657420, 0x666F72, 0x676520, 0x67656E,
                                0x676572, 0x696765, 0x696C20, 0x696E67, 0x6B6520, 0x6B6B65,
                                0x6C6572, 0x6C6967, 0x6C6C65, 0x6D6564, 0x6E6465, 0x6E6520,
                                0x6E6720, 0x6E6765, 0x6F6720, 0x6F6D20, 0x6F7220, 0x70E520,
                                0x722064, 0x722065, 0x722073, 0x726520, 0x737465, 0x742073,
                                0x746520, 0x746572, 0x74696C, 0x766572,}),

                new NGramsPlusLang("de",
                        new int[]{0x20616E, 0x206175, 0x206265, 0x206461, 0x206465, 0x206469,
                                0x206569, 0x206765, 0x206861, 0x20696E, 0x206D69, 0x207363,
                                0x207365, 0x20756E, 0x207665, 0x20766F, 0x207765, 0x207A75,
                                0x626572, 0x636820, 0x636865, 0x636874, 0x646173, 0x64656E,
                                0x646572, 0x646965, 0x652064, 0x652073, 0x65696E, 0x656974,
                                0x656E20, 0x657220, 0x657320, 0x67656E, 0x68656E, 0x687420,
                                0x696368, 0x696520, 0x696E20, 0x696E65, 0x697420, 0x6C6963,
                                0x6C6C65, 0x6E2061, 0x6E2064, 0x6E2073, 0x6E6420, 0x6E6465,
                                0x6E6520, 0x6E6720, 0x6E6765, 0x6E7465, 0x722064, 0x726465,
                                0x726569, 0x736368, 0x737465, 0x742064, 0x746520, 0x74656E,
                                0x746572, 0x756E64, 0x756E67, 0x766572,}),

                new NGramsPlusLang("en",
                        new int[]{0x206120, 0x20616E, 0x206265, 0x20636F, 0x20666F, 0x206861,
                                0x206865, 0x20696E, 0x206D61, 0x206F66, 0x207072, 0x207265,
                                0x207361, 0x207374, 0x207468, 0x20746F, 0x207768, 0x616964,
                                0x616C20, 0x616E20, 0x616E64, 0x617320, 0x617420, 0x617465,
                                0x617469, 0x642061, 0x642074, 0x652061, 0x652073, 0x652074,
                                0x656420, 0x656E74, 0x657220, 0x657320, 0x666F72, 0x686174,
                                0x686520, 0x686572, 0x696420, 0x696E20, 0x696E67, 0x696F6E,
                                0x697320, 0x6E2061, 0x6E2074, 0x6E6420, 0x6E6720, 0x6E7420,
                                0x6F6620, 0x6F6E20, 0x6F7220, 0x726520, 0x727320, 0x732061,
                                0x732074, 0x736169, 0x737420, 0x742074, 0x746572, 0x746861,
                                0x746865, 0x74696F, 0x746F20, 0x747320,}),

                new NGramsPlusLang("es",
                        new int[]{0x206120, 0x206361, 0x20636F, 0x206465, 0x20656C, 0x20656E,
                                0x206573, 0x20696E, 0x206C61, 0x206C6F, 0x207061, 0x20706F,
                                0x207072, 0x207175, 0x207265, 0x207365, 0x20756E, 0x207920,
                                0x612063, 0x612064, 0x612065, 0x61206C, 0x612070, 0x616369,
                                0x61646F, 0x616C20, 0x617220, 0x617320, 0x6369F3, 0x636F6E,
                                0x646520, 0x64656C, 0x646F20, 0x652064, 0x652065, 0x65206C,
                                0x656C20, 0x656E20, 0x656E74, 0x657320, 0x657374, 0x69656E,
                                0x69F36E, 0x6C6120, 0x6C6F73, 0x6E2065, 0x6E7465, 0x6F2064,
                                0x6F2065, 0x6F6E20, 0x6F7220, 0x6F7320, 0x706172, 0x717565,
                                0x726120, 0x726573, 0x732064, 0x732065, 0x732070, 0x736520,
                                0x746520, 0x746F20, 0x756520, 0xF36E20,}),

                new NGramsPlusLang("fr",
                        new int[]{0x206175, 0x20636F, 0x206461, 0x206465, 0x206475, 0x20656E,
                                0x206574, 0x206C61, 0x206C65, 0x207061, 0x20706F, 0x207072,
                                0x207175, 0x207365, 0x20736F, 0x20756E, 0x20E020, 0x616E74,
                                0x617469, 0x636520, 0x636F6E, 0x646520, 0x646573, 0x647520,
                                0x652061, 0x652063, 0x652064, 0x652065, 0x65206C, 0x652070,
                                0x652073, 0x656E20, 0x656E74, 0x657220, 0x657320, 0x657420,
                                0x657572, 0x696F6E, 0x697320, 0x697420, 0x6C6120, 0x6C6520,
                                0x6C6573, 0x6D656E, 0x6E2064, 0x6E6520, 0x6E7320, 0x6E7420,
                                0x6F6E20, 0x6F6E74, 0x6F7572, 0x717565, 0x72206C, 0x726520,
                                0x732061, 0x732064, 0x732065, 0x73206C, 0x732070, 0x742064,
                                0x746520, 0x74696F, 0x756520, 0x757220,}),

                new NGramsPlusLang("it",
                        new int[]{0x20616C, 0x206368, 0x20636F, 0x206465, 0x206469, 0x206520,
                                0x20696C, 0x20696E, 0x206C61, 0x207065, 0x207072, 0x20756E,
                                0x612063, 0x612064, 0x612070, 0x612073, 0x61746F, 0x636865,
                                0x636F6E, 0x64656C, 0x646920, 0x652061, 0x652063, 0x652064,
                                0x652069, 0x65206C, 0x652070, 0x652073, 0x656C20, 0x656C6C,
                                0x656E74, 0x657220, 0x686520, 0x692061, 0x692063, 0x692064,
                                0x692073, 0x696120, 0x696C20, 0x696E20, 0x696F6E, 0x6C6120,
                                0x6C6520, 0x6C6920, 0x6C6C61, 0x6E6520, 0x6E6920, 0x6E6F20,
                                0x6E7465, 0x6F2061, 0x6F2064, 0x6F2069, 0x6F2073, 0x6F6E20,
                                0x6F6E65, 0x706572, 0x726120, 0x726520, 0x736920, 0x746120,
                                0x746520, 0x746920, 0x746F20, 0x7A696F,}),

                new NGramsPlusLang("nl",
                        new int[]{0x20616C, 0x206265, 0x206461, 0x206465, 0x206469, 0x206565,
                                0x20656E, 0x206765, 0x206865, 0x20696E, 0x206D61, 0x206D65,
                                0x206F70, 0x207465, 0x207661, 0x207665, 0x20766F, 0x207765,
                                0x207A69, 0x61616E, 0x616172, 0x616E20, 0x616E64, 0x617220,
                                0x617420, 0x636874, 0x646520, 0x64656E, 0x646572, 0x652062,
                                0x652076, 0x65656E, 0x656572, 0x656E20, 0x657220, 0x657273,
                                0x657420, 0x67656E, 0x686574, 0x696520, 0x696E20, 0x696E67,
                                0x697320, 0x6E2062, 0x6E2064, 0x6E2065, 0x6E2068, 0x6E206F,
                                0x6E2076, 0x6E6465, 0x6E6720, 0x6F6E64, 0x6F6F72, 0x6F7020,
                                0x6F7220, 0x736368, 0x737465, 0x742064, 0x746520, 0x74656E,
                                0x746572, 0x76616E, 0x766572, 0x766F6F,}),

                new NGramsPlusLang("no",
                        new int[]{0x206174, 0x206176, 0x206465, 0x20656E, 0x206572, 0x20666F,
                                0x206861, 0x206920, 0x206D65, 0x206F67, 0x2070E5, 0x207365,
                                0x20736B, 0x20736F, 0x207374, 0x207469, 0x207669, 0x20E520,
                                0x616E64, 0x617220, 0x617420, 0x646520, 0x64656E, 0x646574,
                                0x652073, 0x656420, 0x656E20, 0x656E65, 0x657220, 0x657265,
                                0x657420, 0x657474, 0x666F72, 0x67656E, 0x696B6B, 0x696C20,
                                0x696E67, 0x6B6520, 0x6B6B65, 0x6C6520, 0x6C6C65, 0x6D6564,
                                0x6D656E, 0x6E2073, 0x6E6520, 0x6E6720, 0x6E6765, 0x6E6E65,
                                0x6F6720, 0x6F6D20, 0x6F7220, 0x70E520, 0x722073, 0x726520,
                                0x736F6D, 0x737465, 0x742073, 0x746520, 0x74656E, 0x746572,
                                0x74696C, 0x747420, 0x747465, 0x766572,}),

                new NGramsPlusLang("pt",
                        new int[]{0x206120, 0x20636F, 0x206461, 0x206465, 0x20646F, 0x206520,
                                0x206573, 0x206D61, 0x206E6F, 0x206F20, 0x207061, 0x20706F,
                                0x207072, 0x207175, 0x207265, 0x207365, 0x20756D, 0x612061,
                                0x612063, 0x612064, 0x612070, 0x616465, 0x61646F, 0x616C20,
                                0x617220, 0x617261, 0x617320, 0x636F6D, 0x636F6E, 0x646120,
                                0x646520, 0x646F20, 0x646F73, 0x652061, 0x652064, 0x656D20,
                                0x656E74, 0x657320, 0x657374, 0x696120, 0x696361, 0x6D656E,
                                0x6E7465, 0x6E746F, 0x6F2061, 0x6F2063, 0x6F2064, 0x6F2065,
                                0x6F2070, 0x6F7320, 0x706172, 0x717565, 0x726120, 0x726573,
                                0x732061, 0x732064, 0x732065, 0x732070, 0x737461, 0x746520,
                                0x746F20, 0x756520, 0xE36F20, 0xE7E36F,

                        }),

                new NGramsPlusLang("sv",
                        new int[]{0x206174, 0x206176, 0x206465, 0x20656E, 0x2066F6, 0x206861,
                                0x206920, 0x20696E, 0x206B6F, 0x206D65, 0x206F63, 0x2070E5,
                                0x20736B, 0x20736F, 0x207374, 0x207469, 0x207661, 0x207669,
                                0x20E472, 0x616465, 0x616E20, 0x616E64, 0x617220, 0x617474,
                                0x636820, 0x646520, 0x64656E, 0x646572, 0x646574, 0x656420,
                                0x656E20, 0x657220, 0x657420, 0x66F672, 0x67656E, 0x696C6C,
                                0x696E67, 0x6B6120, 0x6C6C20, 0x6D6564, 0x6E2073, 0x6E6120,
                                0x6E6465, 0x6E6720, 0x6E6765, 0x6E696E, 0x6F6368, 0x6F6D20,
                                0x6F6E20, 0x70E520, 0x722061, 0x722073, 0x726120, 0x736B61,
                                0x736F6D, 0x742073, 0x746120, 0x746520, 0x746572, 0x74696C,
                                0x747420, 0x766172, 0xE47220, 0xF67220,}),

        };


        public CharsetMatch match(CharsetDetector det) {
            String name = det.fC1Bytes ? "windows-1252" : "ISO-8859-1";
            int bestConfidenceSoFar = -1;
            String lang = null;
            for (NGramsPlusLang ngl : ngrams_8859_1) {
                int confidence = match(det, ngl.fNGrams, byteMap);
                if (confidence > bestConfidenceSoFar) {
                    bestConfidenceSoFar = confidence;
                    lang = ngl.fLang;
                }
            }
            return bestConfidenceSoFar <= 0 ? null :
                    new CharsetMatch(det, this, bestConfidenceSoFar, name, lang);
        }


        public String getName() {
            return "ISO-8859-1";
        }
    }


    static class CharsetRecog_8859_2 extends CharsetRecog_sbcs {
        protected static byte[] byteMap =
                {(byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x00, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x61,
                        (byte) 0x62, (byte) 0x63, (byte) 0x64, (byte) 0x65, (byte) 0x66,
                        (byte) 0x67, (byte) 0x68, (byte) 0x69, (byte) 0x6A, (byte) 0x6B,
                        (byte) 0x6C, (byte) 0x6D, (byte) 0x6E, (byte) 0x6F, (byte) 0x70,
                        (byte) 0x71, (byte) 0x72, (byte) 0x73, (byte) 0x74, (byte) 0x75,
                        (byte) 0x76, (byte) 0x77, (byte) 0x78, (byte) 0x79, (byte) 0x7A,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x61, (byte) 0x62, (byte) 0x63, (byte) 0x64,
                        (byte) 0x65, (byte) 0x66, (byte) 0x67, (byte) 0x68, (byte) 0x69,
                        (byte) 0x6A, (byte) 0x6B, (byte) 0x6C, (byte) 0x6D, (byte) 0x6E,
                        (byte) 0x6F, (byte) 0x70, (byte) 0x71, (byte) 0x72, (byte) 0x73,
                        (byte) 0x74, (byte) 0x75, (byte) 0x76, (byte) 0x77, (byte) 0x78,
                        (byte) 0x79, (byte) 0x7A, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0xB1, (byte) 0x20, (byte) 0xB3, (byte) 0x20, (byte) 0xB5,
                        (byte) 0xB6, (byte) 0x20, (byte) 0x20, (byte) 0xB9, (byte) 0xBA,
                        (byte) 0xBB, (byte) 0xBC, (byte) 0x20, (byte) 0xBE, (byte) 0xBF,
                        (byte) 0x20, (byte) 0xB1, (byte) 0x20, (byte) 0xB3, (byte) 0x20,
                        (byte) 0xB5, (byte) 0xB6, (byte) 0xB7, (byte) 0x20, (byte) 0xB9,
                        (byte) 0xBA, (byte) 0xBB, (byte) 0xBC, (byte) 0x20, (byte) 0xBE,
                        (byte) 0xBF, (byte) 0xE0, (byte) 0xE1, (byte) 0xE2, (byte) 0xE3,
                        (byte) 0xE4, (byte) 0xE5, (byte) 0xE6, (byte) 0xE7, (byte) 0xE8,
                        (byte) 0xE9, (byte) 0xEA, (byte) 0xEB, (byte) 0xEC, (byte) 0xED,
                        (byte) 0xEE, (byte) 0xEF, (byte) 0xF0, (byte) 0xF1, (byte) 0xF2,
                        (byte) 0xF3, (byte) 0xF4, (byte) 0xF5, (byte) 0xF6, (byte) 0x20,
                        (byte) 0xF8, (byte) 0xF9, (byte) 0xFA, (byte) 0xFB, (byte) 0xFC,
                        (byte) 0xFD, (byte) 0xFE, (byte) 0xDF, (byte) 0xE0, (byte) 0xE1,
                        (byte) 0xE2, (byte) 0xE3, (byte) 0xE4, (byte) 0xE5, (byte) 0xE6,
                        (byte) 0xE7, (byte) 0xE8, (byte) 0xE9, (byte) 0xEA, (byte) 0xEB,
                        (byte) 0xEC, (byte) 0xED, (byte) 0xEE, (byte) 0xEF, (byte) 0xF0,
                        (byte) 0xF1, (byte) 0xF2, (byte) 0xF3, (byte) 0xF4, (byte) 0xF5,
                        (byte) 0xF6, (byte) 0x20, (byte) 0xF8, (byte) 0xF9, (byte) 0xFA,
                        (byte) 0xFB, (byte) 0xFC, (byte) 0xFD, (byte) 0xFE, (byte) 0x20,};

        private static NGramsPlusLang[] ngrams_8859_2 = new NGramsPlusLang[]{
                new NGramsPlusLang("cs",
                        new int[]{0x206120, 0x206279, 0x20646F, 0x206A65, 0x206E61, 0x206E65,
                                0x206F20, 0x206F64, 0x20706F, 0x207072, 0x2070F8, 0x20726F,
                                0x207365, 0x20736F, 0x207374, 0x20746F, 0x207620, 0x207679,
                                0x207A61, 0x612070, 0x636520, 0x636820, 0x652070, 0x652073,
                                0x652076, 0x656D20, 0x656EED, 0x686F20, 0x686F64, 0x697374,
                                0x6A6520, 0x6B7465, 0x6C6520, 0x6C6920, 0x6E6120, 0x6EE920,
                                0x6EEC20, 0x6EED20, 0x6F2070, 0x6F646E, 0x6F6A69, 0x6F7374,
                                0x6F7520, 0x6F7661, 0x706F64, 0x706F6A, 0x70726F, 0x70F865,
                                0x736520, 0x736F75, 0x737461, 0x737469, 0x73746E, 0x746572,
                                0x746EED, 0x746F20, 0x752070, 0xBE6520, 0xE16EED, 0xE9686F,
                                0xED2070, 0xED2073, 0xED6D20, 0xF86564,}),
                new NGramsPlusLang("hu",
                        new int[]{0x206120, 0x20617A, 0x206265, 0x206567, 0x20656C, 0x206665,
                                0x206861, 0x20686F, 0x206973, 0x206B65, 0x206B69, 0x206BF6,
                                0x206C65, 0x206D61, 0x206D65, 0x206D69, 0x206E65, 0x20737A,
                                0x207465, 0x20E973, 0x612061, 0x61206B, 0x61206D, 0x612073,
                                0x616B20, 0x616E20, 0x617A20, 0x62616E, 0x62656E, 0x656779,
                                0x656B20, 0x656C20, 0x656C65, 0x656D20, 0x656E20, 0x657265,
                                0x657420, 0x657465, 0x657474, 0x677920, 0x686F67, 0x696E74,
                                0x697320, 0x6B2061, 0x6BF67A, 0x6D6567, 0x6D696E, 0x6E2061,
                                0x6E616B, 0x6E656B, 0x6E656D, 0x6E7420, 0x6F6779, 0x732061,
                                0x737A65, 0x737A74, 0x737AE1, 0x73E967, 0x742061, 0x747420,
                                0x74E173, 0x7A6572, 0xE16E20, 0xE97320,}),
                new NGramsPlusLang("pl",
                        new int[]{0x20637A, 0x20646F, 0x206920, 0x206A65, 0x206B6F, 0x206D61,
                                0x206D69, 0x206E61, 0x206E69, 0x206F64, 0x20706F, 0x207072,
                                0x207369, 0x207720, 0x207769, 0x207779, 0x207A20, 0x207A61,
                                0x612070, 0x612077, 0x616E69, 0x636820, 0x637A65, 0x637A79,
                                0x646F20, 0x647A69, 0x652070, 0x652073, 0x652077, 0x65207A,
                                0x65676F, 0x656A20, 0x656D20, 0x656E69, 0x676F20, 0x696120,
                                0x696520, 0x69656A, 0x6B6120, 0x6B6920, 0x6B6965, 0x6D6965,
                                0x6E6120, 0x6E6961, 0x6E6965, 0x6F2070, 0x6F7761, 0x6F7769,
                                0x706F6C, 0x707261, 0x70726F, 0x70727A, 0x727A65, 0x727A79,
                                0x7369EA, 0x736B69, 0x737461, 0x776965, 0x796368, 0x796D20,
                                0x7A6520, 0x7A6965, 0x7A7920, 0xF37720,}),
                new NGramsPlusLang("ro",
                        new int[]{0x206120, 0x206163, 0x206361, 0x206365, 0x20636F, 0x206375,
                                0x206465, 0x206469, 0x206C61, 0x206D61, 0x207065, 0x207072,
                                0x207365, 0x2073E3, 0x20756E, 0x20BA69, 0x20EE6E, 0x612063,
                                0x612064, 0x617265, 0x617420, 0x617465, 0x617520, 0x636172,
                                0x636F6E, 0x637520, 0x63E320, 0x646520, 0x652061, 0x652063,
                                0x652064, 0x652070, 0x652073, 0x656120, 0x656920, 0x656C65,
                                0x656E74, 0x657374, 0x692061, 0x692063, 0x692064, 0x692070,
                                0x696520, 0x696920, 0x696E20, 0x6C6120, 0x6C6520, 0x6C6F72,
                                0x6C7569, 0x6E6520, 0x6E7472, 0x6F7220, 0x70656E, 0x726520,
                                0x726561, 0x727520, 0x73E320, 0x746520, 0x747275, 0x74E320,
                                0x756920, 0x756C20, 0xBA6920, 0xEE6E20,})};

        public CharsetMatch match(CharsetDetector det) {
            String name = det.fC1Bytes ? "windows-1250" : "ISO-8859-2";
            int bestConfidenceSoFar = -1;
            String lang = null;
            for (NGramsPlusLang ngl : ngrams_8859_2) {
                int confidence = match(det, ngl.fNGrams, byteMap);
                if (confidence > bestConfidenceSoFar) {
                    bestConfidenceSoFar = confidence;
                    lang = ngl.fLang;
                }
            }
            return bestConfidenceSoFar <= 0 ? null :
                    new CharsetMatch(det, this, bestConfidenceSoFar, name, lang);
        }

        public String getName() {
            return "ISO-8859-2";
        }

    }


    abstract static class CharsetRecog_8859_5 extends CharsetRecog_sbcs {
        protected static byte[] byteMap =
                {(byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x00, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x61,
                        (byte) 0x62, (byte) 0x63, (byte) 0x64, (byte) 0x65, (byte) 0x66,
                        (byte) 0x67, (byte) 0x68, (byte) 0x69, (byte) 0x6A, (byte) 0x6B,
                        (byte) 0x6C, (byte) 0x6D, (byte) 0x6E, (byte) 0x6F, (byte) 0x70,
                        (byte) 0x71, (byte) 0x72, (byte) 0x73, (byte) 0x74, (byte) 0x75,
                        (byte) 0x76, (byte) 0x77, (byte) 0x78, (byte) 0x79, (byte) 0x7A,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x61, (byte) 0x62, (byte) 0x63, (byte) 0x64,
                        (byte) 0x65, (byte) 0x66, (byte) 0x67, (byte) 0x68, (byte) 0x69,
                        (byte) 0x6A, (byte) 0x6B, (byte) 0x6C, (byte) 0x6D, (byte) 0x6E,
                        (byte) 0x6F, (byte) 0x70, (byte) 0x71, (byte) 0x72, (byte) 0x73,
                        (byte) 0x74, (byte) 0x75, (byte) 0x76, (byte) 0x77, (byte) 0x78,
                        (byte) 0x79, (byte) 0x7A, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0xF1, (byte) 0xF2, (byte) 0xF3, (byte) 0xF4, (byte) 0xF5,
                        (byte) 0xF6, (byte) 0xF7, (byte) 0xF8, (byte) 0xF9, (byte) 0xFA,
                        (byte) 0xFB, (byte) 0xFC, (byte) 0x20, (byte) 0xFE, (byte) 0xFF,
                        (byte) 0xD0, (byte) 0xD1, (byte) 0xD2, (byte) 0xD3, (byte) 0xD4,
                        (byte) 0xD5, (byte) 0xD6, (byte) 0xD7, (byte) 0xD8, (byte) 0xD9,
                        (byte) 0xDA, (byte) 0xDB, (byte) 0xDC, (byte) 0xDD, (byte) 0xDE,
                        (byte) 0xDF, (byte) 0xE0, (byte) 0xE1, (byte) 0xE2, (byte) 0xE3,
                        (byte) 0xE4, (byte) 0xE5, (byte) 0xE6, (byte) 0xE7, (byte) 0xE8,
                        (byte) 0xE9, (byte) 0xEA, (byte) 0xEB, (byte) 0xEC, (byte) 0xED,
                        (byte) 0xEE, (byte) 0xEF, (byte) 0xD0, (byte) 0xD1, (byte) 0xD2,
                        (byte) 0xD3, (byte) 0xD4, (byte) 0xD5, (byte) 0xD6, (byte) 0xD7,
                        (byte) 0xD8, (byte) 0xD9, (byte) 0xDA, (byte) 0xDB, (byte) 0xDC,
                        (byte) 0xDD, (byte) 0xDE, (byte) 0xDF, (byte) 0xE0, (byte) 0xE1,
                        (byte) 0xE2, (byte) 0xE3, (byte) 0xE4, (byte) 0xE5, (byte) 0xE6,
                        (byte) 0xE7, (byte) 0xE8, (byte) 0xE9, (byte) 0xEA, (byte) 0xEB,
                        (byte) 0xEC, (byte) 0xED, (byte) 0xEE, (byte) 0xEF, (byte) 0x20,
                        (byte) 0xF1, (byte) 0xF2, (byte) 0xF3, (byte) 0xF4, (byte) 0xF5,
                        (byte) 0xF6, (byte) 0xF7, (byte) 0xF8, (byte) 0xF9, (byte) 0xFA,
                        (byte) 0xFB, (byte) 0xFC, (byte) 0x20, (byte) 0xFE, (byte) 0xFF,};

        public String getName() {
            return "ISO-8859-5";
        }
    }

    static class CharsetRecog_8859_5_ru extends CharsetRecog_8859_5 {
        private static int[] ngrams =
                {0x20D220, 0x20D2DE, 0x20D4DE, 0x20D7D0, 0x20D820, 0x20DAD0, 0x20DADE, 0x20DDD0,
                        0x20DDD5, 0x20DED1, 0x20DFDE, 0x20DFE0, 0x20E0D0, 0x20E1DE, 0x20E1E2,
                        0x20E2DE, 0x20E7E2, 0x20EDE2, 0xD0DDD8, 0xD0E2EC, 0xD3DE20, 0xD5DBEC,
                        0xD5DDD8, 0xD5E1E2, 0xD5E220, 0xD820DF, 0xD8D520, 0xD8D820, 0xD8EF20,
                        0xDBD5DD, 0xDBD820, 0xDBECDD, 0xDDD020, 0xDDD520, 0xDDD8D5, 0xDDD8EF,
                        0xDDDE20, 0xDDDED2, 0xDE20D2, 0xDE20DF, 0xDE20E1, 0xDED220, 0xDED2D0,
                        0xDED3DE, 0xDED920, 0xDEDBEC, 0xDEDC20, 0xDEE1E2, 0xDFDEDB, 0xDFE0D5,
                        0xDFE0D8, 0xDFE0DE, 0xE0D0D2, 0xE0D5D4, 0xE1E2D0, 0xE1E2D2, 0xE1E2D8,
                        0xE1EF20, 0xE2D5DB, 0xE2DE20, 0xE2DEE0, 0xE2EC20, 0xE7E2DE, 0xEBE520,};

        public String getLanguage() {
            return "ru";
        }

        public CharsetMatch match(CharsetDetector det) {
            int confidence = match(det, ngrams, byteMap);
            return confidence == 0 ? null : new CharsetMatch(det, this, confidence);
        }
    }

    abstract static class CharsetRecog_8859_6 extends CharsetRecog_sbcs {
        protected static byte[] byteMap =
                {(byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x00, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x61,
                        (byte) 0x62, (byte) 0x63, (byte) 0x64, (byte) 0x65, (byte) 0x66,
                        (byte) 0x67, (byte) 0x68, (byte) 0x69, (byte) 0x6A, (byte) 0x6B,
                        (byte) 0x6C, (byte) 0x6D, (byte) 0x6E, (byte) 0x6F, (byte) 0x70,
                        (byte) 0x71, (byte) 0x72, (byte) 0x73, (byte) 0x74, (byte) 0x75,
                        (byte) 0x76, (byte) 0x77, (byte) 0x78, (byte) 0x79, (byte) 0x7A,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x61, (byte) 0x62, (byte) 0x63, (byte) 0x64,
                        (byte) 0x65, (byte) 0x66, (byte) 0x67, (byte) 0x68, (byte) 0x69,
                        (byte) 0x6A, (byte) 0x6B, (byte) 0x6C, (byte) 0x6D, (byte) 0x6E,
                        (byte) 0x6F, (byte) 0x70, (byte) 0x71, (byte) 0x72, (byte) 0x73,
                        (byte) 0x74, (byte) 0x75, (byte) 0x76, (byte) 0x77, (byte) 0x78,
                        (byte) 0x79, (byte) 0x7A, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0xC1, (byte) 0xC2, (byte) 0xC3,
                        (byte) 0xC4, (byte) 0xC5, (byte) 0xC6, (byte) 0xC7, (byte) 0xC8,
                        (byte) 0xC9, (byte) 0xCA, (byte) 0xCB, (byte) 0xCC, (byte) 0xCD,
                        (byte) 0xCE, (byte) 0xCF, (byte) 0xD0, (byte) 0xD1, (byte) 0xD2,
                        (byte) 0xD3, (byte) 0xD4, (byte) 0xD5, (byte) 0xD6, (byte) 0xD7,
                        (byte) 0xD8, (byte) 0xD9, (byte) 0xDA, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0xE0, (byte) 0xE1,
                        (byte) 0xE2, (byte) 0xE3, (byte) 0xE4, (byte) 0xE5, (byte) 0xE6,
                        (byte) 0xE7, (byte) 0xE8, (byte) 0xE9, (byte) 0xEA, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,};

        public String getName() {
            return "ISO-8859-6";
        }
    }

    static class CharsetRecog_8859_6_ar extends CharsetRecog_8859_6 {
        private static int[] ngrams =
                {0x20C7E4, 0x20C7E6, 0x20C8C7, 0x20D9E4, 0x20E1EA, 0x20E4E4, 0x20E5E6, 0x20E8C7,
                        0xC720C7, 0xC7C120, 0xC7CA20, 0xC7D120, 0xC7E420, 0xC7E4C3, 0xC7E4C7,
                        0xC7E4C8, 0xC7E4CA, 0xC7E4CC, 0xC7E4CD, 0xC7E4CF, 0xC7E4D3, 0xC7E4D9,
                        0xC7E4E2, 0xC7E4E5, 0xC7E4E8, 0xC7E4EA, 0xC7E520, 0xC7E620, 0xC7E6CA,
                        0xC820C7, 0xC920C7, 0xC920E1, 0xC920E4, 0xC920E5, 0xC920E8, 0xCA20C7,
                        0xCF20C7, 0xCFC920, 0xD120C7, 0xD1C920, 0xD320C7, 0xD920C7, 0xD9E4E9,
                        0xE1EA20, 0xE420C7, 0xE4C920, 0xE4E920, 0xE4EA20, 0xE520C7, 0xE5C720,
                        0xE5C920, 0xE5E620, 0xE620C7, 0xE720C7, 0xE7C720, 0xE8C7E4, 0xE8E620,
                        0xE920C7, 0xEA20C7, 0xEA20E5, 0xEA20E8, 0xEAC920, 0xEAD120, 0xEAE620,};

        public String getLanguage() {
            return "ar";
        }

        public CharsetMatch match(CharsetDetector det) {
            int confidence = match(det, ngrams, byteMap);
            return confidence == 0 ? null : new CharsetMatch(det, this, confidence);
        }
    }

    abstract static class CharsetRecog_8859_7 extends CharsetRecog_sbcs {
        protected static byte[] byteMap =
                {(byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x00, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x61,
                        (byte) 0x62, (byte) 0x63, (byte) 0x64, (byte) 0x65, (byte) 0x66,
                        (byte) 0x67, (byte) 0x68, (byte) 0x69, (byte) 0x6A, (byte) 0x6B,
                        (byte) 0x6C, (byte) 0x6D, (byte) 0x6E, (byte) 0x6F, (byte) 0x70,
                        (byte) 0x71, (byte) 0x72, (byte) 0x73, (byte) 0x74, (byte) 0x75,
                        (byte) 0x76, (byte) 0x77, (byte) 0x78, (byte) 0x79, (byte) 0x7A,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x61, (byte) 0x62, (byte) 0x63, (byte) 0x64,
                        (byte) 0x65, (byte) 0x66, (byte) 0x67, (byte) 0x68, (byte) 0x69,
                        (byte) 0x6A, (byte) 0x6B, (byte) 0x6C, (byte) 0x6D, (byte) 0x6E,
                        (byte) 0x6F, (byte) 0x70, (byte) 0x71, (byte) 0x72, (byte) 0x73,
                        (byte) 0x74, (byte) 0x75, (byte) 0x76, (byte) 0x77, (byte) 0x78,
                        (byte) 0x79, (byte) 0x7A, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0xA1, (byte) 0xA2, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0xDC, (byte) 0x20, (byte) 0xDD, (byte) 0xDE,
                        (byte) 0xDF, (byte) 0x20, (byte) 0xFC, (byte) 0x20, (byte) 0xFD,
                        (byte) 0xFE, (byte) 0xC0, (byte) 0xE1, (byte) 0xE2, (byte) 0xE3,
                        (byte) 0xE4, (byte) 0xE5, (byte) 0xE6, (byte) 0xE7, (byte) 0xE8,
                        (byte) 0xE9, (byte) 0xEA, (byte) 0xEB, (byte) 0xEC, (byte) 0xED,
                        (byte) 0xEE, (byte) 0xEF, (byte) 0xF0, (byte) 0xF1, (byte) 0x20,
                        (byte) 0xF3, (byte) 0xF4, (byte) 0xF5, (byte) 0xF6, (byte) 0xF7,
                        (byte) 0xF8, (byte) 0xF9, (byte) 0xFA, (byte) 0xFB, (byte) 0xDC,
                        (byte) 0xDD, (byte) 0xDE, (byte) 0xDF, (byte) 0xE0, (byte) 0xE1,
                        (byte) 0xE2, (byte) 0xE3, (byte) 0xE4, (byte) 0xE5, (byte) 0xE6,
                        (byte) 0xE7, (byte) 0xE8, (byte) 0xE9, (byte) 0xEA, (byte) 0xEB,
                        (byte) 0xEC, (byte) 0xED, (byte) 0xEE, (byte) 0xEF, (byte) 0xF0,
                        (byte) 0xF1, (byte) 0xF2, (byte) 0xF3, (byte) 0xF4, (byte) 0xF5,
                        (byte) 0xF6, (byte) 0xF7, (byte) 0xF8, (byte) 0xF9, (byte) 0xFA,
                        (byte) 0xFB, (byte) 0xFC, (byte) 0xFD, (byte) 0xFE, (byte) 0x20,};

        public String getName() {
            return "ISO-8859-7";
        }
    }

    static class CharsetRecog_8859_7_el extends CharsetRecog_8859_7 {
        private static int[] ngrams =
                {0x20E1ED, 0x20E1F0, 0x20E3E9, 0x20E4E9, 0x20E5F0, 0x20E720, 0x20EAE1, 0x20ECE5,
                        0x20EDE1, 0x20EF20, 0x20F0E1, 0x20F0EF, 0x20F0F1, 0x20F3F4, 0x20F3F5,
                        0x20F4E7, 0x20F4EF, 0xDFE120, 0xE120E1, 0xE120F4, 0xE1E920, 0xE1ED20,
                        0xE1F0FC, 0xE1F220, 0xE3E9E1, 0xE5E920, 0xE5F220, 0xE720F4, 0xE7ED20,
                        0xE7F220, 0xE920F4, 0xE9E120, 0xE9EADE, 0xE9F220, 0xEAE1E9, 0xEAE1F4,
                        0xECE520, 0xED20E1, 0xED20E5, 0xED20F0, 0xEDE120, 0xEFF220, 0xEFF520,
                        0xF0EFF5, 0xF0F1EF, 0xF0FC20, 0xF220E1, 0xF220E5, 0xF220EA, 0xF220F0,
                        0xF220F4, 0xF3E520, 0xF3E720, 0xF3F4EF, 0xF4E120, 0xF4E1E9, 0xF4E7ED,
                        0xF4E7F2, 0xF4E9EA, 0xF4EF20, 0xF4EFF5, 0xF4F9ED, 0xF9ED20, 0xFEED20,};

        public String getLanguage() {
            return "el";
        }

        public CharsetMatch match(CharsetDetector det) {
            String name = det.fC1Bytes ? "windows-1253" : "ISO-8859-7";
            int confidence = match(det, ngrams, byteMap);
            return confidence == 0 ? null : new CharsetMatch(det, this, confidence, name, "el");
        }
    }

    abstract static class CharsetRecog_8859_8 extends CharsetRecog_sbcs {
        protected static byte[] byteMap =
                {(byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x00, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x61,
                        (byte) 0x62, (byte) 0x63, (byte) 0x64, (byte) 0x65, (byte) 0x66,
                        (byte) 0x67, (byte) 0x68, (byte) 0x69, (byte) 0x6A, (byte) 0x6B,
                        (byte) 0x6C, (byte) 0x6D, (byte) 0x6E, (byte) 0x6F, (byte) 0x70,
                        (byte) 0x71, (byte) 0x72, (byte) 0x73, (byte) 0x74, (byte) 0x75,
                        (byte) 0x76, (byte) 0x77, (byte) 0x78, (byte) 0x79, (byte) 0x7A,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x61, (byte) 0x62, (byte) 0x63, (byte) 0x64,
                        (byte) 0x65, (byte) 0x66, (byte) 0x67, (byte) 0x68, (byte) 0x69,
                        (byte) 0x6A, (byte) 0x6B, (byte) 0x6C, (byte) 0x6D, (byte) 0x6E,
                        (byte) 0x6F, (byte) 0x70, (byte) 0x71, (byte) 0x72, (byte) 0x73,
                        (byte) 0x74, (byte) 0x75, (byte) 0x76, (byte) 0x77, (byte) 0x78,
                        (byte) 0x79, (byte) 0x7A, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0xB5, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0xE0, (byte) 0xE1,
                        (byte) 0xE2, (byte) 0xE3, (byte) 0xE4, (byte) 0xE5, (byte) 0xE6,
                        (byte) 0xE7, (byte) 0xE8, (byte) 0xE9, (byte) 0xEA, (byte) 0xEB,
                        (byte) 0xEC, (byte) 0xED, (byte) 0xEE, (byte) 0xEF, (byte) 0xF0,
                        (byte) 0xF1, (byte) 0xF2, (byte) 0xF3, (byte) 0xF4, (byte) 0xF5,
                        (byte) 0xF6, (byte) 0xF7, (byte) 0xF8, (byte) 0xF9, (byte) 0xFA,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,};

        public String getName() {
            return "ISO-8859-8";
        }
    }

    static class CharsetRecog_8859_8_I_he extends CharsetRecog_8859_8 {
        private static int[] ngrams =
                {0x20E0E5, 0x20E0E7, 0x20E0E9, 0x20E0FA, 0x20E1E9, 0x20E1EE, 0x20E4E0, 0x20E4E5,
                        0x20E4E9, 0x20E4EE, 0x20E4F2, 0x20E4F9, 0x20E4FA, 0x20ECE0, 0x20ECE4,
                        0x20EEE0, 0x20F2EC, 0x20F9EC, 0xE0FA20, 0xE420E0, 0xE420E1, 0xE420E4,
                        0xE420EC, 0xE420EE, 0xE420F9, 0xE4E5E0, 0xE5E020, 0xE5ED20, 0xE5EF20,
                        0xE5F820, 0xE5FA20, 0xE920E4, 0xE9E420, 0xE9E5FA, 0xE9E9ED, 0xE9ED20,
                        0xE9EF20, 0xE9F820, 0xE9FA20, 0xEC20E0, 0xEC20E4, 0xECE020, 0xECE420,
                        0xED20E0, 0xED20E1, 0xED20E4, 0xED20EC, 0xED20EE, 0xED20F9, 0xEEE420,
                        0xEF20E4, 0xF0E420, 0xF0E920, 0xF0E9ED, 0xF2EC20, 0xF820E4, 0xF8E9ED,
                        0xF9EC20, 0xFA20E0, 0xFA20E1, 0xFA20E4, 0xFA20EC, 0xFA20EE, 0xFA20F9,};

        public String getName() {
            return "ISO-8859-8-I";
        }

        public String getLanguage() {
            return "he";
        }

        public CharsetMatch match(CharsetDetector det) {
            String name = det.fC1Bytes ? "windows-1255" : "ISO-8859-8-I";
            int confidence = match(det, ngrams, byteMap);
            return confidence == 0 ? null : new CharsetMatch(det, this, confidence, name, "he");
        }
    }

    static class CharsetRecog_8859_8_he extends CharsetRecog_8859_8 {
        private static int[] ngrams =
                {0x20E0E5, 0x20E0EC, 0x20E4E9, 0x20E4EC, 0x20E4EE, 0x20E4F0, 0x20E9F0, 0x20ECF2,
                        0x20ECF9, 0x20EDE5, 0x20EDE9, 0x20EFE5, 0x20EFE9, 0x20F8E5, 0x20F8E9,
                        0x20FAE0, 0x20FAE5, 0x20FAE9, 0xE020E4, 0xE020EC, 0xE020ED, 0xE020FA,
                        0xE0E420, 0xE0E5E4, 0xE0EC20, 0xE0EE20, 0xE120E4, 0xE120ED, 0xE120FA,
                        0xE420E4, 0xE420E9, 0xE420EC, 0xE420ED, 0xE420EF, 0xE420F8, 0xE420FA,
                        0xE4EC20, 0xE5E020, 0xE5E420, 0xE7E020, 0xE9E020, 0xE9E120, 0xE9E420,
                        0xEC20E4, 0xEC20ED, 0xEC20FA, 0xECF220, 0xECF920, 0xEDE9E9, 0xEDE9F0,
                        0xEDE9F8, 0xEE20E4, 0xEE20ED, 0xEE20FA, 0xEEE120, 0xEEE420, 0xF2E420,
                        0xF920E4, 0xF920ED, 0xF920FA, 0xF9E420, 0xFAE020, 0xFAE420, 0xFAE5E9,};

        public String getLanguage() {
            return "he";
        }

        public CharsetMatch match(CharsetDetector det) {
            String name = det.fC1Bytes ? "windows-1255" : "ISO-8859-8";
            int confidence = match(det, ngrams, byteMap);
            return confidence == 0 ? null : new CharsetMatch(det, this, confidence, name, "he");

        }
    }

    abstract static class CharsetRecog_8859_9 extends CharsetRecog_sbcs {
        protected static byte[] byteMap =
                {(byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x00, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x61,
                        (byte) 0x62, (byte) 0x63, (byte) 0x64, (byte) 0x65, (byte) 0x66,
                        (byte) 0x67, (byte) 0x68, (byte) 0x69, (byte) 0x6A, (byte) 0x6B,
                        (byte) 0x6C, (byte) 0x6D, (byte) 0x6E, (byte) 0x6F, (byte) 0x70,
                        (byte) 0x71, (byte) 0x72, (byte) 0x73, (byte) 0x74, (byte) 0x75,
                        (byte) 0x76, (byte) 0x77, (byte) 0x78, (byte) 0x79, (byte) 0x7A,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x61, (byte) 0x62, (byte) 0x63, (byte) 0x64,
                        (byte) 0x65, (byte) 0x66, (byte) 0x67, (byte) 0x68, (byte) 0x69,
                        (byte) 0x6A, (byte) 0x6B, (byte) 0x6C, (byte) 0x6D, (byte) 0x6E,
                        (byte) 0x6F, (byte) 0x70, (byte) 0x71, (byte) 0x72, (byte) 0x73,
                        (byte) 0x74, (byte) 0x75, (byte) 0x76, (byte) 0x77, (byte) 0x78,
                        (byte) 0x79, (byte) 0x7A, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0xAA,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0xB5, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0xBA, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0xE0, (byte) 0xE1, (byte) 0xE2, (byte) 0xE3,
                        (byte) 0xE4, (byte) 0xE5, (byte) 0xE6, (byte) 0xE7, (byte) 0xE8,
                        (byte) 0xE9, (byte) 0xEA, (byte) 0xEB, (byte) 0xEC, (byte) 0xED,
                        (byte) 0xEE, (byte) 0xEF, (byte) 0xF0, (byte) 0xF1, (byte) 0xF2,
                        (byte) 0xF3, (byte) 0xF4, (byte) 0xF5, (byte) 0xF6, (byte) 0x20,
                        (byte) 0xF8, (byte) 0xF9, (byte) 0xFA, (byte) 0xFB, (byte) 0xFC,
                        (byte) 0x69, (byte) 0xFE, (byte) 0xDF, (byte) 0xE0, (byte) 0xE1,
                        (byte) 0xE2, (byte) 0xE3, (byte) 0xE4, (byte) 0xE5, (byte) 0xE6,
                        (byte) 0xE7, (byte) 0xE8, (byte) 0xE9, (byte) 0xEA, (byte) 0xEB,
                        (byte) 0xEC, (byte) 0xED, (byte) 0xEE, (byte) 0xEF, (byte) 0xF0,
                        (byte) 0xF1, (byte) 0xF2, (byte) 0xF3, (byte) 0xF4, (byte) 0xF5,
                        (byte) 0xF6, (byte) 0x20, (byte) 0xF8, (byte) 0xF9, (byte) 0xFA,
                        (byte) 0xFB, (byte) 0xFC, (byte) 0xFD, (byte) 0xFE, (byte) 0xFF,};

        public String getName() {
            return "ISO-8859-9";
        }
    }

    static class CharsetRecog_8859_9_tr extends CharsetRecog_8859_9 {
        private static int[] ngrams =
                {0x206261, 0x206269, 0x206275, 0x206461, 0x206465, 0x206765, 0x206861, 0x20696C,
                        0x206B61, 0x206B6F, 0x206D61, 0x206F6C, 0x207361, 0x207461, 0x207665,
                        0x207961, 0x612062, 0x616B20, 0x616C61, 0x616D61, 0x616E20, 0x616EFD,
                        0x617220, 0x617261, 0x6172FD, 0x6173FD, 0x617961, 0x626972, 0x646120,
                        0x646520, 0x646920, 0x652062, 0x65206B, 0x656469, 0x656E20, 0x657220,
                        0x657269, 0x657369, 0x696C65, 0x696E20, 0x696E69, 0x697220, 0x6C616E,
                        0x6C6172, 0x6C6520, 0x6C6572, 0x6E2061, 0x6E2062, 0x6E206B, 0x6E6461,
                        0x6E6465, 0x6E6520, 0x6E6920, 0x6E696E, 0x6EFD20, 0x72696E, 0x72FD6E,
                        0x766520, 0x796120, 0x796F72, 0xFD6E20, 0xFD6E64, 0xFD6EFD, 0xFDF0FD,};

        public String getLanguage() {
            return "tr";
        }

        public CharsetMatch match(CharsetDetector det) {
            String name = det.fC1Bytes ? "windows-1254" : "ISO-8859-9";
            int confidence = match(det, ngrams, byteMap);
            return confidence == 0 ? null : new CharsetMatch(det, this, confidence, name, "tr");
        }
    }

    static class CharsetRecog_windows_1251 extends CharsetRecog_sbcs {
        private static int[] ngrams =
                {0x20E220, 0x20E2EE, 0x20E4EE, 0x20E7E0, 0x20E820, 0x20EAE0, 0x20EAEE, 0x20EDE0,
                        0x20EDE5, 0x20EEE1, 0x20EFEE, 0x20EFF0, 0x20F0E0, 0x20F1EE, 0x20F1F2,
                        0x20F2EE, 0x20F7F2, 0x20FDF2, 0xE0EDE8, 0xE0F2FC, 0xE3EE20, 0xE5EBFC,
                        0xE5EDE8, 0xE5F1F2, 0xE5F220, 0xE820EF, 0xE8E520, 0xE8E820, 0xE8FF20,
                        0xEBE5ED, 0xEBE820, 0xEBFCED, 0xEDE020, 0xEDE520, 0xEDE8E5, 0xEDE8FF,
                        0xEDEE20, 0xEDEEE2, 0xEE20E2, 0xEE20EF, 0xEE20F1, 0xEEE220, 0xEEE2E0,
                        0xEEE3EE, 0xEEE920, 0xEEEBFC, 0xEEEC20, 0xEEF1F2, 0xEFEEEB, 0xEFF0E5,
                        0xEFF0E8, 0xEFF0EE, 0xF0E0E2, 0xF0E5E4, 0xF1F2E0, 0xF1F2E2, 0xF1F2E8,
                        0xF1FF20, 0xF2E5EB, 0xF2EE20, 0xF2EEF0, 0xF2FC20, 0xF7F2EE, 0xFBF520,};

        private static byte[] byteMap =
                {(byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x00, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x61,
                        (byte) 0x62, (byte) 0x63, (byte) 0x64, (byte) 0x65, (byte) 0x66,
                        (byte) 0x67, (byte) 0x68, (byte) 0x69, (byte) 0x6A, (byte) 0x6B,
                        (byte) 0x6C, (byte) 0x6D, (byte) 0x6E, (byte) 0x6F, (byte) 0x70,
                        (byte) 0x71, (byte) 0x72, (byte) 0x73, (byte) 0x74, (byte) 0x75,
                        (byte) 0x76, (byte) 0x77, (byte) 0x78, (byte) 0x79, (byte) 0x7A,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x61, (byte) 0x62, (byte) 0x63, (byte) 0x64,
                        (byte) 0x65, (byte) 0x66, (byte) 0x67, (byte) 0x68, (byte) 0x69,
                        (byte) 0x6A, (byte) 0x6B, (byte) 0x6C, (byte) 0x6D, (byte) 0x6E,
                        (byte) 0x6F, (byte) 0x70, (byte) 0x71, (byte) 0x72, (byte) 0x73,
                        (byte) 0x74, (byte) 0x75, (byte) 0x76, (byte) 0x77, (byte) 0x78,
                        (byte) 0x79, (byte) 0x7A, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x90, (byte) 0x83, (byte) 0x20,
                        (byte) 0x83, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x9A, (byte) 0x20, (byte) 0x9C,
                        (byte) 0x9D, (byte) 0x9E, (byte) 0x9F, (byte) 0x90, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x9A, (byte) 0x20,
                        (byte) 0x9C, (byte) 0x9D, (byte) 0x9E, (byte) 0x9F, (byte) 0x20,
                        (byte) 0xA2, (byte) 0xA2, (byte) 0xBC, (byte) 0x20, (byte) 0xB4,
                        (byte) 0x20, (byte) 0x20, (byte) 0xB8, (byte) 0x20, (byte) 0xBA,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0xBF,
                        (byte) 0x20, (byte) 0x20, (byte) 0xB3, (byte) 0xB3, (byte) 0xB4,
                        (byte) 0xB5, (byte) 0x20, (byte) 0x20, (byte) 0xB8, (byte) 0x20,
                        (byte) 0xBA, (byte) 0x20, (byte) 0xBC, (byte) 0xBE, (byte) 0xBE,
                        (byte) 0xBF, (byte) 0xE0, (byte) 0xE1, (byte) 0xE2, (byte) 0xE3,
                        (byte) 0xE4, (byte) 0xE5, (byte) 0xE6, (byte) 0xE7, (byte) 0xE8,
                        (byte) 0xE9, (byte) 0xEA, (byte) 0xEB, (byte) 0xEC, (byte) 0xED,
                        (byte) 0xEE, (byte) 0xEF, (byte) 0xF0, (byte) 0xF1, (byte) 0xF2,
                        (byte) 0xF3, (byte) 0xF4, (byte) 0xF5, (byte) 0xF6, (byte) 0xF7,
                        (byte) 0xF8, (byte) 0xF9, (byte) 0xFA, (byte) 0xFB, (byte) 0xFC,
                        (byte) 0xFD, (byte) 0xFE, (byte) 0xFF, (byte) 0xE0, (byte) 0xE1,
                        (byte) 0xE2, (byte) 0xE3, (byte) 0xE4, (byte) 0xE5, (byte) 0xE6,
                        (byte) 0xE7, (byte) 0xE8, (byte) 0xE9, (byte) 0xEA, (byte) 0xEB,
                        (byte) 0xEC, (byte) 0xED, (byte) 0xEE, (byte) 0xEF, (byte) 0xF0,
                        (byte) 0xF1, (byte) 0xF2, (byte) 0xF3, (byte) 0xF4, (byte) 0xF5,
                        (byte) 0xF6, (byte) 0xF7, (byte) 0xF8, (byte) 0xF9, (byte) 0xFA,
                        (byte) 0xFB, (byte) 0xFC, (byte) 0xFD, (byte) 0xFE, (byte) 0xFF,};

        public String getName() {
            return "windows-1251";
        }

        public String getLanguage() {
            return "ru";
        }

        public CharsetMatch match(CharsetDetector det) {
            int confidence = match(det, ngrams, byteMap);
            return confidence == 0 ? null :
                    new CharsetMatch(det, this, confidence, getName(), "ru");
        }
    }

    static class CharsetRecog_IBM866_ru extends CharsetRecog_sbcs {
        private static int[] ngrams =
                {0x20E220, 0x20E2EE, 0x20E4EE, 0x20E7E0, 0x20E820, 0x20EAE0, 0x20EAEE, 0x20EDE0,
                        0x20EDE5, 0x20EEE1, 0x20EFEE, 0x20EFF0, 0x20F0E0, 0x20F1EE, 0x20F1F2,
                        0x20F2EE, 0x20F7F2, 0x20FDF2, 0xE0EDE8, 0xE0F2FC, 0xE3EE20, 0xE5EBFC,
                        0xE5EDE8, 0xE5F1F2, 0xE5F220, 0xE820EF, 0xE8E520, 0xE8E820, 0xE8FF20,
                        0xEBE5ED, 0xEBE820, 0xEBFCED, 0xEDE020, 0xEDE520, 0xEDE8E5, 0xEDE8FF,
                        0xEDEE20, 0xEDEEE2, 0xEE20E2, 0xEE20EF, 0xEE20F1, 0xEEE220, 0xEEE2E0,
                        0xEEE3EE, 0xEEE920, 0xEEEBFC, 0xEEEC20, 0xEEF1F2, 0xEFEEEB, 0xEFF0E5,
                        0xEFF0E8, 0xEFF0EE, 0xF0E0E2, 0xF0E5E4, 0xF1F2E0, 0xF1F2E2, 0xF1F2E8,
                        0xF1FF20, 0xF2E5EB, 0xF2EE20, 0xF2EEF0, 0xF2FC20, 0xF7F2EE, 0xFBF520,};

        // bytemap converts cp866 chars to cp1251 chars, so ngrams are still unchanged
        private static byte[] byteMap =
                {(byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x00, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x61,
                        (byte) 0x62, (byte) 0x63, (byte) 0x64, (byte) 0x65, (byte) 0x66,
                        (byte) 0x67, (byte) 0x68, (byte) 0x69, (byte) 0x6A, (byte) 0x6B,
                        (byte) 0x6C, (byte) 0x6D, (byte) 0x6E, (byte) 0x6F, (byte) 0x70,
                        (byte) 0x71, (byte) 0x72, (byte) 0x73, (byte) 0x74, (byte) 0x75,
                        (byte) 0x76, (byte) 0x77, (byte) 0x78, (byte) 0x79, (byte) 0x7A,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x61, (byte) 0x62, (byte) 0x63, (byte) 0x64,
                        (byte) 0x65, (byte) 0x66, (byte) 0x67, (byte) 0x68, (byte) 0x69,
                        (byte) 0x6A, (byte) 0x6B, (byte) 0x6C, (byte) 0x6D, (byte) 0x6E,
                        (byte) 0x6F, (byte) 0x70, (byte) 0x71, (byte) 0x72, (byte) 0x73,
                        (byte) 0x74, (byte) 0x75, (byte) 0x76, (byte) 0x77, (byte) 0x78,
                        (byte) 0x79, (byte) 0x7A, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0xE0, (byte) 0xE1, (byte) 0xE2,
                        (byte) 0xE3, (byte) 0xE4, (byte) 0xE5, (byte) 0xE6, (byte) 0xE7,
                        (byte) 0xE8, (byte) 0xE9, (byte) 0xEA, (byte) 0xEB, (byte) 0xEC,
                        (byte) 0xED, (byte) 0xEE, (byte) 0xEF, (byte) 0xF0, (byte) 0xF1,
                        (byte) 0xF2, (byte) 0xF3, (byte) 0xF4, (byte) 0xF5, (byte) 0xF6,
                        (byte) 0xF7, (byte) 0xF8, (byte) 0xF9, (byte) 0xFA, (byte) 0xFB,
                        (byte) 0xFC, (byte) 0xFD, (byte) 0xFE, (byte) 0xFF, (byte) 0xE0,
                        (byte) 0xE1, (byte) 0xE2, (byte) 0xE3, (byte) 0xE4, (byte) 0xE5,
                        (byte) 0xE6, (byte) 0xE7, (byte) 0xE8, (byte) 0xE9, (byte) 0xEA,
                        (byte) 0xEB, (byte) 0xEC, (byte) 0xED, (byte) 0xEE, (byte) 0xEF,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0xF0, (byte) 0xF1,
                        (byte) 0xF2, (byte) 0xF3, (byte) 0xF4, (byte) 0xF5, (byte) 0xF6,
                        (byte) 0xF7, (byte) 0xF8, (byte) 0xF9, (byte) 0xFA, (byte) 0xFB,
                        (byte) 0xFC, (byte) 0xFD, (byte) 0xFE, (byte) 0xFF, (byte) 0xB8,
                        (byte) 0xB8, (byte) 0xBA, (byte) 0xBA, (byte) 0xBF, (byte) 0xBF,
                        (byte) 0xA2, (byte) 0xA2, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,};

        public String getName() {
            return "IBM866";
        }

        public String getLanguage() {
            return "ru";
        }

        public CharsetMatch match(CharsetDetector det) {
            int confidence = match(det, ngrams, byteMap);
            return confidence == 0 ? null : new CharsetMatch(det, this, confidence);
        }
    }

    static class CharsetRecog_windows_1256 extends CharsetRecog_sbcs {
        private static int[] ngrams =
                {0x20C7E1, 0x20C7E4, 0x20C8C7, 0x20DAE1, 0x20DDED, 0x20E1E1, 0x20E3E4, 0x20E6C7,
                        0xC720C7, 0xC7C120, 0xC7CA20, 0xC7D120, 0xC7E120, 0xC7E1C3, 0xC7E1C7,
                        0xC7E1C8, 0xC7E1CA, 0xC7E1CC, 0xC7E1CD, 0xC7E1CF, 0xC7E1D3, 0xC7E1DA,
                        0xC7E1DE, 0xC7E1E3, 0xC7E1E6, 0xC7E1ED, 0xC7E320, 0xC7E420, 0xC7E4CA,
                        0xC820C7, 0xC920C7, 0xC920DD, 0xC920E1, 0xC920E3, 0xC920E6, 0xCA20C7,
                        0xCF20C7, 0xCFC920, 0xD120C7, 0xD1C920, 0xD320C7, 0xDA20C7, 0xDAE1EC,
                        0xDDED20, 0xE120C7, 0xE1C920, 0xE1EC20, 0xE1ED20, 0xE320C7, 0xE3C720,
                        0xE3C920, 0xE3E420, 0xE420C7, 0xE520C7, 0xE5C720, 0xE6C7E1, 0xE6E420,
                        0xEC20C7, 0xED20C7, 0xED20E3, 0xED20E6, 0xEDC920, 0xEDD120, 0xEDE420,};

        private static byte[] byteMap =
                {(byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x00, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x61,
                        (byte) 0x62, (byte) 0x63, (byte) 0x64, (byte) 0x65, (byte) 0x66,
                        (byte) 0x67, (byte) 0x68, (byte) 0x69, (byte) 0x6A, (byte) 0x6B,
                        (byte) 0x6C, (byte) 0x6D, (byte) 0x6E, (byte) 0x6F, (byte) 0x70,
                        (byte) 0x71, (byte) 0x72, (byte) 0x73, (byte) 0x74, (byte) 0x75,
                        (byte) 0x76, (byte) 0x77, (byte) 0x78, (byte) 0x79, (byte) 0x7A,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x61, (byte) 0x62, (byte) 0x63, (byte) 0x64,
                        (byte) 0x65, (byte) 0x66, (byte) 0x67, (byte) 0x68, (byte) 0x69,
                        (byte) 0x6A, (byte) 0x6B, (byte) 0x6C, (byte) 0x6D, (byte) 0x6E,
                        (byte) 0x6F, (byte) 0x70, (byte) 0x71, (byte) 0x72, (byte) 0x73,
                        (byte) 0x74, (byte) 0x75, (byte) 0x76, (byte) 0x77, (byte) 0x78,
                        (byte) 0x79, (byte) 0x7A, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x81, (byte) 0x20,
                        (byte) 0x83, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x88, (byte) 0x20, (byte) 0x8A, (byte) 0x20, (byte) 0x9C,
                        (byte) 0x8D, (byte) 0x8E, (byte) 0x8F, (byte) 0x90, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x98, (byte) 0x20, (byte) 0x9A, (byte) 0x20,
                        (byte) 0x9C, (byte) 0x20, (byte) 0x20, (byte) 0x9F, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0xAA,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0xB5, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0xC0, (byte) 0xC1, (byte) 0xC2, (byte) 0xC3,
                        (byte) 0xC4, (byte) 0xC5, (byte) 0xC6, (byte) 0xC7, (byte) 0xC8,
                        (byte) 0xC9, (byte) 0xCA, (byte) 0xCB, (byte) 0xCC, (byte) 0xCD,
                        (byte) 0xCE, (byte) 0xCF, (byte) 0xD0, (byte) 0xD1, (byte) 0xD2,
                        (byte) 0xD3, (byte) 0xD4, (byte) 0xD5, (byte) 0xD6, (byte) 0x20,
                        (byte) 0xD8, (byte) 0xD9, (byte) 0xDA, (byte) 0xDB, (byte) 0xDC,
                        (byte) 0xDD, (byte) 0xDE, (byte) 0xDF, (byte) 0xE0, (byte) 0xE1,
                        (byte) 0xE2, (byte) 0xE3, (byte) 0xE4, (byte) 0xE5, (byte) 0xE6,
                        (byte) 0xE7, (byte) 0xE8, (byte) 0xE9, (byte) 0xEA, (byte) 0xEB,
                        (byte) 0xEC, (byte) 0xED, (byte) 0xEE, (byte) 0xEF, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0xF4, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0xF9, (byte) 0x20,
                        (byte) 0xFB, (byte) 0xFC, (byte) 0x20, (byte) 0x20, (byte) 0xFF,};

        public String getName() {
            return "windows-1256";
        }

        public String getLanguage() {
            return "ar";
        }

        public CharsetMatch match(CharsetDetector det) {
            int confidence = match(det, ngrams, byteMap);
            return confidence == 0 ? null : new CharsetMatch(det, this, confidence);
        }
    }

    static class CharsetRecog_KOI8_R extends CharsetRecog_sbcs {
        private static int[] ngrams =
                {0x20C4CF, 0x20C920, 0x20CBC1, 0x20CBCF, 0x20CEC1, 0x20CEC5, 0x20CFC2, 0x20D0CF,
                        0x20D0D2, 0x20D2C1, 0x20D3CF, 0x20D3D4, 0x20D4CF, 0x20D720, 0x20D7CF,
                        0x20DAC1, 0x20DCD4, 0x20DED4, 0xC1CEC9, 0xC1D4D8, 0xC5CCD8, 0xC5CEC9,
                        0xC5D3D4, 0xC5D420, 0xC7CF20, 0xC920D0, 0xC9C520, 0xC9C920, 0xC9D120,
                        0xCCC5CE, 0xCCC920, 0xCCD8CE, 0xCEC120, 0xCEC520, 0xCEC9C5, 0xCEC9D1,
                        0xCECF20, 0xCECFD7, 0xCF20D0, 0xCF20D3, 0xCF20D7, 0xCFC7CF, 0xCFCA20,
                        0xCFCCD8, 0xCFCD20, 0xCFD3D4, 0xCFD720, 0xCFD7C1, 0xD0CFCC, 0xD0D2C5,
                        0xD0D2C9, 0xD0D2CF, 0xD2C1D7, 0xD2C5C4, 0xD3D120, 0xD3D4C1, 0xD3D4C9,
                        0xD3D4D7, 0xD4C5CC, 0xD4CF20, 0xD4CFD2, 0xD4D820, 0xD9C820, 0xDED4CF,};

        private static byte[] byteMap =
                {(byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x00, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x61,
                        (byte) 0x62, (byte) 0x63, (byte) 0x64, (byte) 0x65, (byte) 0x66,
                        (byte) 0x67, (byte) 0x68, (byte) 0x69, (byte) 0x6A, (byte) 0x6B,
                        (byte) 0x6C, (byte) 0x6D, (byte) 0x6E, (byte) 0x6F, (byte) 0x70,
                        (byte) 0x71, (byte) 0x72, (byte) 0x73, (byte) 0x74, (byte) 0x75,
                        (byte) 0x76, (byte) 0x77, (byte) 0x78, (byte) 0x79, (byte) 0x7A,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x61, (byte) 0x62, (byte) 0x63, (byte) 0x64,
                        (byte) 0x65, (byte) 0x66, (byte) 0x67, (byte) 0x68, (byte) 0x69,
                        (byte) 0x6A, (byte) 0x6B, (byte) 0x6C, (byte) 0x6D, (byte) 0x6E,
                        (byte) 0x6F, (byte) 0x70, (byte) 0x71, (byte) 0x72, (byte) 0x73,
                        (byte) 0x74, (byte) 0x75, (byte) 0x76, (byte) 0x77, (byte) 0x78,
                        (byte) 0x79, (byte) 0x7A, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0xA3, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0xA3, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                        (byte) 0x20, (byte) 0xC0, (byte) 0xC1, (byte) 0xC2, (byte) 0xC3,
                        (byte) 0xC4, (byte) 0xC5, (byte) 0xC6, (byte) 0xC7, (byte) 0xC8,
                        (byte) 0xC9, (byte) 0xCA, (byte) 0xCB, (byte) 0xCC, (byte) 0xCD,
                        (byte) 0xCE, (byte) 0xCF, (byte) 0xD0, (byte) 0xD1, (byte) 0xD2,
                        (byte) 0xD3, (byte) 0xD4, (byte) 0xD5, (byte) 0xD6, (byte) 0xD7,
                        (byte) 0xD8, (byte) 0xD9, (byte) 0xDA, (byte) 0xDB, (byte) 0xDC,
                        (byte) 0xDD, (byte) 0xDE, (byte) 0xDF, (byte) 0xC0, (byte) 0xC1,
                        (byte) 0xC2, (byte) 0xC3, (byte) 0xC4, (byte) 0xC5, (byte) 0xC6,
                        (byte) 0xC7, (byte) 0xC8, (byte) 0xC9, (byte) 0xCA, (byte) 0xCB,
                        (byte) 0xCC, (byte) 0xCD, (byte) 0xCE, (byte) 0xCF, (byte) 0xD0,
                        (byte) 0xD1, (byte) 0xD2, (byte) 0xD3, (byte) 0xD4, (byte) 0xD5,
                        (byte) 0xD6, (byte) 0xD7, (byte) 0xD8, (byte) 0xD9, (byte) 0xDA,
                        (byte) 0xDB, (byte) 0xDC, (byte) 0xDD, (byte) 0xDE, (byte) 0xDF,};

        public String getName() {
            return "KOI8-R";
        }

        public String getLanguage() {
            return "ru";
        }

        public CharsetMatch match(CharsetDetector det) {
            int confidence = match(det, ngrams, byteMap);
            return confidence == 0 ? null : new CharsetMatch(det, this, confidence);
        }
    }

    abstract static class CharsetRecog_IBM424_he extends CharsetRecog_sbcs {
        protected static byte[] byteMap = {
/*
 -0           -1           -2           -3           -4
        -5           -6           -7           -8           -9           -A
                  -B           -C           -D           -E           -F
                  */
/* 0- */    (byte) 0x40, (byte) 0x40, (byte) 0x40, (byte) 0x40, (byte) 0x40, (byte) 0x40,
                (byte) 0x40, (byte) 0x40, (byte) 0x40, (byte) 0x40, (byte) 0x40, (byte) 0x40,
                (byte) 0x40, (byte) 0x40, (byte) 0x40, (byte) 0x40,
/* 1- */    (byte) 0x40, (byte) 0x40, (byte) 0x40, (byte) 0x40, (byte) 0x40, (byte) 0x40,
                (byte) 0x40, (byte) 0x40, (byte) 0x40, (byte) 0x40, (byte) 0x40, (byte) 0x40,
                (byte) 0x40, (byte) 0x40, (byte) 0x40, (byte) 0x40,
/* 2- */    (byte) 0x40, (byte) 0x40, (byte) 0x40, (byte) 0x40, (byte) 0x40, (byte) 0x40,
                (byte) 0x40, (byte) 0x40, (byte) 0x40, (byte) 0x40, (byte) 0x40, (byte) 0x40,
                (byte) 0x40, (byte) 0x40, (byte) 0x40, (byte) 0x40,
/* 3- */    (byte) 0x40, (byte) 0x40, (byte) 0x40, (byte) 0x40, (byte) 0x40, (byte) 0x40,
                (byte) 0x40, (byte) 0x40, (byte) 0x40, (byte) 0x40, (byte) 0x40, (byte) 0x40,
                (byte) 0x40, (byte) 0x40, (byte) 0x40, (byte) 0x40,
/* 4- */    (byte) 0x40, (byte) 0x41, (byte) 0x42, (byte) 0x43, (byte) 0x44, (byte) 0x45,
                (byte) 0x46, (byte) 0x47, (byte) 0x48, (byte) 0x49, (byte) 0x40, (byte) 0x40,
                (byte) 0x40, (byte) 0x40, (byte) 0x40, (byte) 0x40,
/* 5- */    (byte) 0x40, (byte) 0x51, (byte) 0x52, (byte) 0x53, (byte) 0x54, (byte) 0x55,
                (byte) 0x56, (byte) 0x57, (byte) 0x58, (byte) 0x59, (byte) 0x40, (byte) 0x40,
                (byte) 0x40, (byte) 0x40, (byte) 0x40, (byte) 0x40,
/* 6- */    (byte) 0x40, (byte) 0x40, (byte) 0x62, (byte) 0x63, (byte) 0x64, (byte) 0x65,
                (byte) 0x66, (byte) 0x67, (byte) 0x68, (byte) 0x69, (byte) 0x40, (byte) 0x40,
                (byte) 0x40, (byte) 0x40, (byte) 0x40, (byte) 0x40,
/* 7- */    (byte) 0x40, (byte) 0x71, (byte) 0x40, (byte) 0x40, (byte) 0x40, (byte) 0x40,
                (byte) 0x40, (byte) 0x40, (byte) 0x40, (byte) 0x40, (byte) 0x40, (byte) 0x40,
                (byte) 0x40, (byte) 0x00, (byte) 0x40, (byte) 0x40,
/* 8- */    (byte) 0x40, (byte) 0x81, (byte) 0x82, (byte) 0x83, (byte) 0x84, (byte) 0x85,
                (byte) 0x86, (byte) 0x87, (byte) 0x88, (byte) 0x89, (byte) 0x40, (byte) 0x40,
                (byte) 0x40, (byte) 0x40, (byte) 0x40, (byte) 0x40,
/* 9- */    (byte) 0x40, (byte) 0x91, (byte) 0x92, (byte) 0x93, (byte) 0x94, (byte) 0x95,
                (byte) 0x96, (byte) 0x97, (byte) 0x98, (byte) 0x99, (byte) 0x40, (byte) 0x40,
                (byte) 0x40, (byte) 0x40, (byte) 0x40, (byte) 0x40,
/* A- */    (byte) 0xA0, (byte) 0x40, (byte) 0xA2, (byte) 0xA3, (byte) 0xA4, (byte) 0xA5,
                (byte) 0xA6, (byte) 0xA7, (byte) 0xA8, (byte) 0xA9, (byte) 0x40, (byte) 0x40,
                (byte) 0x40, (byte) 0x40, (byte) 0x40, (byte) 0x40,
/* B- */    (byte) 0x40, (byte) 0x40, (byte) 0x40, (byte) 0x40, (byte) 0x40, (byte) 0x40,
                (byte) 0x40, (byte) 0x40, (byte) 0x40, (byte) 0x40, (byte) 0x40, (byte) 0x40,
                (byte) 0x40, (byte) 0x40, (byte) 0x40, (byte) 0x40,
/* C- */    (byte) 0x40, (byte) 0x81, (byte) 0x82, (byte) 0x83, (byte) 0x84, (byte) 0x85,
                (byte) 0x86, (byte) 0x87, (byte) 0x88, (byte) 0x89, (byte) 0x40, (byte) 0x40,
                (byte) 0x40, (byte) 0x40, (byte) 0x40, (byte) 0x40,
/* D- */    (byte) 0x40, (byte) 0x91, (byte) 0x92, (byte) 0x93, (byte) 0x94, (byte) 0x95,
                (byte) 0x96, (byte) 0x97, (byte) 0x98, (byte) 0x99, (byte) 0x40, (byte) 0x40,
                (byte) 0x40, (byte) 0x40, (byte) 0x40, (byte) 0x40,
/* E- */    (byte) 0x40, (byte) 0x40, (byte) 0xA2, (byte) 0xA3, (byte) 0xA4, (byte) 0xA5,
                (byte) 0xA6, (byte) 0xA7, (byte) 0xA8, (byte) 0xA9, (byte) 0x40, (byte) 0x40,
                (byte) 0x40, (byte) 0x40, (byte) 0x40, (byte) 0x40,
/* F- */    (byte) 0x40, (byte) 0x40, (byte) 0x40, (byte) 0x40, (byte) 0x40, (byte) 0x40,
                (byte) 0x40, (byte) 0x40, (byte) 0x40, (byte) 0x40, (byte) 0x40, (byte) 0x40,
                (byte) 0x40, (byte) 0x40, (byte) 0x40, (byte) 0x40,};

        public String getLanguage() {
            return "he";
        }
    }

    static class CharsetRecog_IBM424_he_rtl extends CharsetRecog_IBM424_he {
        private static int[] ngrams =
                {0x404146, 0x404148, 0x404151, 0x404171, 0x404251, 0x404256, 0x404541, 0x404546,
                        0x404551, 0x404556, 0x404562, 0x404569, 0x404571, 0x405441, 0x405445,
                        0x405641, 0x406254, 0x406954, 0x417140, 0x454041, 0x454042, 0x454045,
                        0x454054, 0x454056, 0x454069, 0x454641, 0x464140, 0x465540, 0x465740,
                        0x466840, 0x467140, 0x514045, 0x514540, 0x514671, 0x515155, 0x515540,
                        0x515740, 0x516840, 0x517140, 0x544041, 0x544045, 0x544140, 0x544540,
                        0x554041, 0x554042, 0x554045, 0x554054, 0x554056, 0x554069, 0x564540,
                        0x574045, 0x584540, 0x585140, 0x585155, 0x625440, 0x684045, 0x685155,
                        0x695440, 0x714041, 0x714042, 0x714045, 0x714054, 0x714056, 0x714069,};

        public String getName() {
            return "IBM424_rtl";
        }

        public CharsetMatch match(CharsetDetector det) {
            int confidence = match(det, ngrams, byteMap, (byte) 0x40);
            return confidence == 0 ? null : new CharsetMatch(det, this, confidence);
        }
    }

    static class CharsetRecog_IBM424_he_ltr extends CharsetRecog_IBM424_he {
        private static int[] ngrams =
                {0x404146, 0x404154, 0x404551, 0x404554, 0x404556, 0x404558, 0x405158, 0x405462,
                        0x405469, 0x405546, 0x405551, 0x405746, 0x405751, 0x406846, 0x406851,
                        0x407141, 0x407146, 0x407151, 0x414045, 0x414054, 0x414055, 0x414071,
                        0x414540, 0x414645, 0x415440, 0x415640, 0x424045, 0x424055, 0x424071,
                        0x454045, 0x454051, 0x454054, 0x454055, 0x454057, 0x454068, 0x454071,
                        0x455440, 0x464140, 0x464540, 0x484140, 0x514140, 0x514240, 0x514540,
                        0x544045, 0x544055, 0x544071, 0x546240, 0x546940, 0x555151, 0x555158,
                        0x555168, 0x564045, 0x564055, 0x564071, 0x564240, 0x564540, 0x624540,
                        0x694045, 0x694055, 0x694071, 0x694540, 0x714140, 0x714540, 0x714651

                };

        public String getName() {
            return "IBM424_ltr";
        }

        public CharsetMatch match(CharsetDetector det) {
            int confidence = match(det, ngrams, byteMap, (byte) 0x40);
            return confidence == 0 ? null : new CharsetMatch(det, this, confidence);
        }
    }

    abstract static class CharsetRecog_IBM420_ar extends CharsetRecog_sbcs {

        protected static byte[] byteMap = {
/*                 -0           -1           -2
-3           -4           -5           -6           -7           -8
-9           -A           -B           -C           -D           -E           -F   */
/* 0- */    (byte) 0x40, (byte) 0x40, (byte) 0x40, (byte) 0x40, (byte) 0x40, (byte) 0x40,
                (byte) 0x40, (byte) 0x40, (byte) 0x40, (byte) 0x40, (byte) 0x40, (byte) 0x40,
                (byte) 0x40, (byte) 0x40, (byte) 0x40, (byte) 0x40,
/* 1- */    (byte) 0x40, (byte) 0x40, (byte) 0x40, (byte) 0x40, (byte) 0x40, (byte) 0x40,
                (byte) 0x40, (byte) 0x40, (byte) 0x40, (byte) 0x40, (byte) 0x40, (byte) 0x40,
                (byte) 0x40, (byte) 0x40, (byte) 0x40, (byte) 0x40,
/* 2- */    (byte) 0x40, (byte) 0x40, (byte) 0x40, (byte) 0x40, (byte) 0x40, (byte) 0x40,
                (byte) 0x40, (byte) 0x40, (byte) 0x40, (byte) 0x40, (byte) 0x40, (byte) 0x40,
                (byte) 0x40, (byte) 0x40, (byte) 0x40, (byte) 0x40,
/* 3- */    (byte) 0x40, (byte) 0x40, (byte) 0x40, (byte) 0x40, (byte) 0x40, (byte) 0x40,
                (byte) 0x40, (byte) 0x40, (byte) 0x40, (byte) 0x40, (byte) 0x40, (byte) 0x40,
                (byte) 0x40, (byte) 0x40, (byte) 0x40, (byte) 0x40,
/* 4- */    (byte) 0x40, (byte) 0x40, (byte) 0x42, (byte) 0x43, (byte) 0x44, (byte) 0x45,
                (byte) 0x46, (byte) 0x47, (byte) 0x48, (byte) 0x49, (byte) 0x40, (byte) 0x40,
                (byte) 0x40, (byte) 0x40, (byte) 0x40, (byte) 0x40,
/* 5- */    (byte) 0x40, (byte) 0x51, (byte) 0x52, (byte) 0x40, (byte) 0x40, (byte) 0x55,
                (byte) 0x56, (byte) 0x57, (byte) 0x58, (byte) 0x59, (byte) 0x40, (byte) 0x40,
                (byte) 0x40, (byte) 0x40, (byte) 0x40, (byte) 0x40,
/* 6- */    (byte) 0x40, (byte) 0x40, (byte) 0x62, (byte) 0x63, (byte) 0x64, (byte) 0x65,
                (byte) 0x66, (byte) 0x67, (byte) 0x68, (byte) 0x69, (byte) 0x40, (byte) 0x40,
                (byte) 0x40, (byte) 0x40, (byte) 0x40, (byte) 0x40,
/* 7- */    (byte) 0x70, (byte) 0x71, (byte) 0x72, (byte) 0x73, (byte) 0x74, (byte) 0x75,
                (byte) 0x76, (byte) 0x77, (byte) 0x78, (byte) 0x79, (byte) 0x40, (byte) 0x40,
                (byte) 0x40, (byte) 0x40, (byte) 0x40, (byte) 0x40,
/* 8- */    (byte) 0x80, (byte) 0x81, (byte) 0x82, (byte) 0x83, (byte) 0x84, (byte) 0x85,
                (byte) 0x86, (byte) 0x87, (byte) 0x88, (byte) 0x89, (byte) 0x8A, (byte) 0x8B,
                (byte) 0x8C, (byte) 0x8D, (byte) 0x8E, (byte) 0x8F,
/* 9- */    (byte) 0x90, (byte) 0x91, (byte) 0x92, (byte) 0x93, (byte) 0x94, (byte) 0x95,
                (byte) 0x96, (byte) 0x97, (byte) 0x98, (byte) 0x99, (byte) 0x9A, (byte) 0x9B,
                (byte) 0x9C, (byte) 0x9D, (byte) 0x9E, (byte) 0x9F,
/* A- */    (byte) 0xA0, (byte) 0x40, (byte) 0xA2, (byte) 0xA3, (byte) 0xA4, (byte) 0xA5,
                (byte) 0xA6, (byte) 0xA7, (byte) 0xA8, (byte) 0xA9, (byte) 0xAA, (byte) 0xAB,
                (byte) 0xAC, (byte) 0xAD, (byte) 0xAE, (byte) 0xAF,
/* B- */    (byte) 0xB0, (byte) 0xB1, (byte) 0xB2, (byte) 0xB3, (byte) 0xB4, (byte) 0xB5,
                (byte) 0x40, (byte) 0x40, (byte) 0xB8, (byte) 0xB9, (byte) 0xBA, (byte) 0xBB,
                (byte) 0xBC, (byte) 0xBD, (byte) 0xBE, (byte) 0xBF,
/* C- */    (byte) 0x40, (byte) 0x81, (byte) 0x82, (byte) 0x83, (byte) 0x84, (byte) 0x85,
                (byte) 0x86, (byte) 0x87, (byte) 0x88, (byte) 0x89, (byte) 0x40, (byte) 0xCB,
                (byte) 0x40, (byte) 0xCD, (byte) 0x40, (byte) 0xCF,
/* D- */    (byte) 0x40, (byte) 0x91, (byte) 0x92, (byte) 0x93, (byte) 0x94, (byte) 0x95,
                (byte) 0x96, (byte) 0x97, (byte) 0x98, (byte) 0x99, (byte) 0xDA, (byte) 0xDB,
                (byte) 0xDC, (byte) 0xDD, (byte) 0xDE, (byte) 0xDF,
/* E- */    (byte) 0x40, (byte) 0x40, (byte) 0xA2, (byte) 0xA3, (byte) 0xA4, (byte) 0xA5,
                (byte) 0xA6, (byte) 0xA7, (byte) 0xA8, (byte) 0xA9, (byte) 0xEA, (byte) 0xEB,
                (byte) 0x40, (byte) 0xED, (byte) 0xEE, (byte) 0xEF,
/* F- */    (byte) 0x40, (byte) 0x40, (byte) 0x40, (byte) 0x40, (byte) 0x40, (byte) 0x40,
                (byte) 0x40, (byte) 0x40, (byte) 0x40, (byte) 0x40, (byte) 0x40, (byte) 0xFB,
                (byte) 0xFC, (byte) 0xFD, (byte) 0xFE, (byte) 0x40,};


        public String getLanguage() {
            return "ar";
        }

    }

    static class CharsetRecog_IBM420_ar_rtl extends CharsetRecog_IBM420_ar {
        private static int[] ngrams =
                {0x4056B1, 0x4056BD, 0x405856, 0x409AB1, 0x40ABDC, 0x40B1B1, 0x40BBBD, 0x40CF56,
                        0x564056, 0x564640, 0x566340, 0x567540, 0x56B140, 0x56B149, 0x56B156,
                        0x56B158, 0x56B163, 0x56B167, 0x56B169, 0x56B173, 0x56B178, 0x56B19A,
                        0x56B1AD, 0x56B1BB, 0x56B1CF, 0x56B1DC, 0x56BB40, 0x56BD40, 0x56BD63,
                        0x584056, 0x624056, 0x6240AB, 0x6240B1, 0x6240BB, 0x6240CF, 0x634056,
                        0x734056, 0x736240, 0x754056, 0x756240, 0x784056, 0x9A4056, 0x9AB1DA,
                        0xABDC40, 0xB14056, 0xB16240, 0xB1DA40, 0xB1DC40, 0xBB4056, 0xBB5640,
                        0xBB6240, 0xBBBD40, 0xBD4056, 0xBF4056, 0xBF5640, 0xCF56B1, 0xCFBD40,
                        0xDA4056, 0xDC4056, 0xDC40BB, 0xDC40CF, 0xDC6240, 0xDC7540, 0xDCBD40,};

        public String getName() {
            return "IBM420_rtl";
        }

        public CharsetMatch match(CharsetDetector det) {
            int confidence = matchIBM420(det, ngrams, byteMap, (byte) 0x40);
            return confidence == 0 ? null : new CharsetMatch(det, this, confidence);
        }

    }

    static class CharsetRecog_IBM420_ar_ltr extends CharsetRecog_IBM420_ar {
        private static int[] ngrams =
                {0x404656, 0x4056BB, 0x4056BF, 0x406273, 0x406275, 0x4062B1, 0x4062BB, 0x4062DC,
                        0x406356, 0x407556, 0x4075DC, 0x40B156, 0x40BB56, 0x40BD56, 0x40BDBB,
                        0x40BDCF, 0x40BDDC, 0x40DAB1, 0x40DCAB, 0x40DCB1, 0x49B156, 0x564056,
                        0x564058, 0x564062, 0x564063, 0x564073, 0x564075, 0x564078, 0x56409A,
                        0x5640B1, 0x5640BB, 0x5640BD, 0x5640BF, 0x5640DA, 0x5640DC, 0x565840,
                        0x56B156, 0x56CF40, 0x58B156, 0x63B156, 0x63BD56, 0x67B156, 0x69B156,
                        0x73B156, 0x78B156, 0x9AB156, 0xAB4062, 0xADB156, 0xB14062, 0xB15640,
                        0xB156CF, 0xB19A40, 0xB1B140, 0xBB4062, 0xBB40DC, 0xBBB156, 0xBD5640,
                        0xBDBB40, 0xCF4062, 0xCF40DC, 0xCFB156, 0xDAB19A, 0xDCAB40, 0xDCB156};

        public String getName() {
            return "IBM420_ltr";
        }

        public CharsetMatch match(CharsetDetector det) {
            int confidence = matchIBM420(det, ngrams, byteMap, (byte) 0x40);
            return confidence == 0 ? null : new CharsetMatch(det, this, confidence);
        }

    }

    static abstract class CharsetRecog_EBCDIC_500 extends CharsetRecog_sbcs {

        // This maps EBCDIC 500 codepoints onto either space (not of interest), or a lower
        //  case ISO_8859_1 number/letter/accented-letter codepoint for ngram matching
        // Because we map to ISO_8859_1, we can re-use the ngrams from those detectors
        // To avoid mis-detection, we skip many of the control characters in the 0x00-0x3f range
        protected static byte[] byteMap = {
/* 0x00-0x07 */ (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00,
/* 0x08-0x0f */ (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                (byte) 0x20, (byte) 0x20,
/* 0x10-0x17 */ (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                (byte) 0x20, (byte) 0x20,
/* 0x18-0x1f */ (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                (byte) 0x20, (byte) 0x20,
/* 0x20-0x27 */ (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                (byte) 0x20, (byte) 0x20,
/* 0x28-0x2f */ (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                (byte) 0x00, (byte) 0x00,
/* 0x30-0x37 */ (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00,
/* 0x38-0x3f */ (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00,
/* 0x40-0x47 */ (byte) 0x20, (byte) 0x20, (byte) 0xe2, (byte) 0xe4, (byte) 0xe0, (byte) 0xe1,
                (byte) 0xe3, (byte) 0xe5,
/* 0x48-0x4f */ (byte) 0xe7, (byte) 0xf1, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                (byte) 0x20, (byte) 0x20,
/* 0x50-0x57 */ (byte) 0x20, (byte) 0xe9, (byte) 0xea, (byte) 0xeb, (byte) 0xe8, (byte) 0xed,
                (byte) 0xee, (byte) 0xef,
/* 0x58-0x5f */ (byte) 0xec, (byte) 0xdf, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                (byte) 0x20, (byte) 0x20,
/* 0x60-0x67 */ (byte) 0x20, (byte) 0x20, (byte) 0xe2, (byte) 0xe4, (byte) 0xe0, (byte) 0xe1,
                (byte) 0xe3, (byte) 0xe5,
/* 0x68-0x6f */ (byte) 0xe7, (byte) 0xf1, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                (byte) 0x20, (byte) 0x20,
/* 0x70-0x77 */ (byte) 0xf8, (byte) 0xe9, (byte) 0xea, (byte) 0xeb, (byte) 0xe8, (byte) 0xed,
                (byte) 0xee, (byte) 0xef,
/* 0x78-0x7f */ (byte) 0xec, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                (byte) 0x20, (byte) 0x20,
/* 0x80-0x87 */ (byte) 0xd8, (byte) 'a', (byte) 'b', (byte) 'c', (byte) 'd', (byte) 'e', (byte) 'f',
                (byte) 'g',
/* 0x88-0x8f */ (byte) 'h', (byte) 'i', (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                (byte) 0x20, (byte) 0x20,
/* 0x90-0x97 */ (byte) 0x20, (byte) 'j', (byte) 'k', (byte) 'l', (byte) 'm', (byte) 'n', (byte) 'o',
                (byte) 'p',
/* 0x98-0x9f */ (byte) 'q', (byte) 'r', (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                (byte) 0x20, (byte) 0x20,
/* 0xa0-0xa7 */ (byte) 0x20, (byte) 0x20, (byte) 's', (byte) 't', (byte) 'u', (byte) 'v',
                (byte) 'w', (byte) 'x',
/* 0xa8-0xaf */ (byte) 'y', (byte) 'z', (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                (byte) 0x20, (byte) 0x20,
/* 0xb0-0xb7 */ (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                (byte) 0x20, (byte) 0x20,
/* 0xb8-0xbf */ (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
                (byte) 0x20, (byte) 0x20,
/* 0xc0-0xc7 */ (byte) 0x20, (byte) 'a', (byte) 'b', (byte) 'c', (byte) 'd', (byte) 'e', (byte) 'f',
                (byte) 'g',
/* 0xc8-0xcf */ (byte) 'h', (byte) 'i', (byte) 0x20, (byte) 0xf4, (byte) 0xf6, (byte) 0xf2,
                (byte) 0xf3, (byte) 0xf5,
/* 0xd0-0xd7 */ (byte) 0x20, (byte) 'j', (byte) 'k', (byte) 'l', (byte) 'm', (byte) 'n', (byte) 'o',
                (byte) 'p',
/* 0xd8-0xdf */ (byte) 'q', (byte) 'r', (byte) 0x20, (byte) 0xfb, (byte) 0xfc, (byte) 0xf9,
                (byte) 0xfa, (byte) 0xff,
/* 0xe0-0xe7 */ (byte) 0x20, (byte) 0x20, (byte) 's', (byte) 't', (byte) 'u', (byte) 'v',
                (byte) 'w', (byte) 'x',
/* 0xe8-0xef */ (byte) 'y', (byte) 'z', (byte) 0x20, (byte) 0xf4, (byte) 0xf6, (byte) 0xf2,
                (byte) 0xf3, (byte) 0xf5,
/* 0xf0-0xf7 */ (byte) '0', (byte) '1', (byte) '2', (byte) '3', (byte) '4', (byte) '5', (byte) '6',
                (byte) '7',
/* 0xf8-0xff */ (byte) '8', (byte) '9', (byte) 0x20, (byte) 0xfb, (byte) 0xfc, (byte) 0xf9,
                (byte) 0xfa, (byte) 0x20,};
        private final int langIndex;

        protected CharsetRecog_EBCDIC_500(int langIndex) {
            this.langIndex = langIndex;
        }

        /**
         * @param lang language to find
         * @return the index into CharsetRecog_8859_1.ngrams_8859_1 that matches his language;
         * throws IllegalArgumentException if language can't be found
         */
        static int findLangIndex(String lang) {
            for (int i = 0; i < CharsetRecog_8859_1.ngrams_8859_1.length; i++) {
                NGramsPlusLang ngpl = CharsetRecog_8859_1.ngrams_8859_1[i];
                if (ngpl.fLang.equals(lang)) {
                    return i;
                }
            }
            throw new IllegalArgumentException("can't find language: " + lang);
        }

        public String getName() {
            return "IBM500";
        }

        public CharsetMatch match(CharsetDetector det) {
            int confidence =
                    match(det, CharsetRecog_8859_1.ngrams_8859_1[getLangIndex()].fNGrams, byteMap);
            return confidence == 0 ? null : new CharsetMatch(det, this, confidence);
        }

        int getLangIndex() {
            return langIndex;
        }


    }

    //The EBCDIC codes were removed from ICU4js trunk as of at least July 26, 2016.
    static class CharsetRecog_EBCDIC_500_en extends CharsetRecog_EBCDIC_500 {

        CharsetRecog_EBCDIC_500_en() {
            super(findLangIndex("en"));
        }

        public String getLanguage() {
            return "en";
        }


    }

    static class CharsetRecog_EBCDIC_500_de extends CharsetRecog_EBCDIC_500 {
        CharsetRecog_EBCDIC_500_de() {
            super(findLangIndex("de"));
        }

        public String getLanguage() {
            return "de";
        }

    }

    static class CharsetRecog_EBCDIC_500_fr extends CharsetRecog_EBCDIC_500 {
        CharsetRecog_EBCDIC_500_fr() {
            super(findLangIndex("fr"));
        }

        public String getLanguage() {
            return "fr";
        }
    }

    static class CharsetRecog_EBCDIC_500_es extends CharsetRecog_EBCDIC_500 {
        CharsetRecog_EBCDIC_500_es() {
            super(findLangIndex("es"));
        }

        public String getLanguage() {
            return "es";
        }
    }

    static class CharsetRecog_EBCDIC_500_it extends CharsetRecog_EBCDIC_500 {
        CharsetRecog_EBCDIC_500_it() {
            super(findLangIndex("it"));
        }

        public String getLanguage() {
            return "it";
        }
    }

    static class CharsetRecog_EBCDIC_500_nl extends CharsetRecog_EBCDIC_500 {
        CharsetRecog_EBCDIC_500_nl() {
            super(findLangIndex("nl"));
        }

        public String getLanguage() {
            return "nl";
        }

    }
}
