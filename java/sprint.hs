import System.IO
import System.Environment
import Control.Monad
import Data.Char

import Sprockell.System

main = do
	text <- readFile "program.sir"
	args <- getArgs
	let cores = intOrEmpty args
	let content = lines text
	let result = map (bobeRead . reverse . dropWhile isSpace . reverse . dropWhile isSpace) content
	run cores result
	
intOrEmpty :: [String] -> Int
intOrEmpty [] = 1
intOrEmpty as = (intRead . head) as

bobeRead :: String -> Instruction
bobeRead = read

intRead :: String -> Int
intRead = read
